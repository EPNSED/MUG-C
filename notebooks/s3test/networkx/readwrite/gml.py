# encoding: utf-8
#    Copyright (C) 2008-2019 by
#    Aric Hagberg <hagberg@lanl.gov>
#    Dan Schult <dschult@colgate.edu>
#    Pieter Swart <swart@lanl.gov>
#    All rights reserved.
#    BSD license.
#
# Author: Aric Hagberg (hagberg@lanl.gov)
"""
Read graphs in GML format.

"GML, the G>raph Modelling Language, is our proposal for a portable
file format for graphs. GML's key features are portability, simple
syntax, extensibility and flexibility. A GML file consists of a
hierarchical key-value lists. Graphs can be annotated with arbitrary
data structures. The idea for a common file format was born at the
GD'95; this proposal is the outcome of many discussions. GML is the
standard file format in the Graphlet graph editor system. It has been
overtaken and adapted by several other systems for drawing graphs."

GML files are stored using a 7-bit ASCII encoding with any extended
ASCII characters (iso8859-1) appearing as HTML character entities.
You will need to give some thought into how the exported data should
interact with different languages and even different Python versions.
Re-importing from gml is also a concern.

Without specifying a `stringizer`/`destringizer`, the code is capable of
handling `int`/`float`/`str`/`dict`/`list` data as required by the GML
specification.  For other data types, you need to explicitly supply a
`stringizer`/`destringizer`.

For better interoperability of data generated by Python 2 and Python 3,
we've provided `literal_stringizer` and `literal_destringizer`.

For additional documentation on the GML file format, please see the
`GML website <http://www.infosun.fim.uni-passau.de/Graphlet/GML/gml-tr.html>`_.

Several example graphs in GML format may be found on Mark Newman's
`Network data page <http://www-personal.umich.edu/~mejn/netdata/>`_.
"""
try:
    try:
        from cStringIO import StringIO
    except ImportError:
        from StringIO import StringIO
except ImportError:
    from io import StringIO
from ast import literal_eval
from collections import defaultdict
import networkx as nx
from networkx.exception import NetworkXError
from networkx.utils import open_file

import re
try:
    import htmlentitydefs
except ImportError:
    # Python 3.x
    import html.entities as htmlentitydefs

__all__ = ['read_gml', 'parse_gml', 'generate_gml', 'write_gml']


try:
    long
except NameError:
    long = int
try:
    unicode
except NameError:
    unicode = str
try:
    unichr
except NameError:
    unichr = chr
try:
    literal_eval(r"u'\u4444'")
except SyntaxError:
    # Remove 'u' prefixes in unicode literals in Python 3
    def rtp_fix_unicode(s): return s[1:]
else:
    rtp_fix_unicode = None


def escape(text):
    """Use XML character references to escape characters.

    Use XML character references for unprintable or non-ASCII
    characters, double quotes and ampersands in a string
    """
    def fixup(m):
        ch = m.group(0)
        return '&#' + str(ord(ch)) + ';'

    text = re.sub('[^ -~]|[&"]', fixup, text)
    return text if isinstance(text, str) else str(text)


def unescape(text):
    """Replace XML character references with the referenced characters"""
    def fixup(m):
        text = m.group(0)
        if text[1] == '#':
            # Character reference
            if text[2] == 'x':
                code = int(text[3:-1], 16)
            else:
                code = int(text[2:-1])
        else:
            # Named entity
            try:
                code = htmlentitydefs.name2codepoint[text[1:-1]]
            except KeyError:
                return text  # leave unchanged
        try:
            return chr(code) if code < 256 else unichr(code)
        except (ValueError, OverflowError):
            return text  # leave unchanged

    return re.sub("&(?:[0-9A-Za-z]+|#(?:[0-9]+|x[0-9A-Fa-f]+));", fixup, text)


def literal_destringizer(rep):
    """Convert a Python literal to the value it represents.

    Parameters
    ----------
    rep : string
        A Python literal.

    Returns
    -------
    value : object
        The value of the Python literal.

    Raises
    ------
    ValueError
        If `rep` is not a Python literal.
    """
    if isinstance(rep, (str, unicode)):
        orig_rep = rep
        if rtp_fix_unicode is not None:
            rep = rtp_fix_unicode(rep)
        try:
            return literal_eval(rep)
        except SyntaxError:
            raise ValueError('%r is not a valid Python literal' % (orig_rep,))
    else:
        raise ValueError('%r is not a string' % (rep,))


@open_file(0, mode='rb')
def read_gml(path, label='label', destringizer=None):
    """Read graph in GML format from `path`.

    Parameters
    ----------
    path : filename or filehandle
        The filename or filehandle to read from.

    label : string, optional
        If not None, the parsed nodes will be renamed according to node
        attributes indicated by `label`. Default value: 'label'.

    destringizer : callable, optional
        A `destringizer` that recovers values stored as strings in GML. If it
        cannot convert a string to a value, a `ValueError` is raised. Default
        value : None.

    Returns
    -------
    G : NetworkX graph
        The parsed graph.

    Raises
    ------
    NetworkXError
        If the input cannot be parsed.

    See Also
    --------
    write_gml, parse_gml, literal_destringizer

    Notes
    -----
    GML files are stored using a 7-bit ASCII encoding with any extended
    ASCII characters (iso8859-1) appearing as HTML character entities.
    Without specifying a `stringizer`/`destringizer`, the code is capable of
    handling `int`/`float`/`str`/`dict`/`list` data as required by the GML
    specification.  For other data types, you need to explicitly supply a
    `stringizer`/`destringizer`.

    For additional documentation on the GML file format, please see the
    `GML website <http://www.infosun.fim.uni-passau.de/Graphlet/GML/gml-tr.html>`_.

    See the module docstring :mod:`networkx.readwrite.gml` for more details.

    Examples
    --------
    >>> G = nx.path_graph(4)
    >>> nx.write_gml(G, 'test.gml')
    >>> H = nx.read_gml('test.gml')
    """
    def filter_lines(lines):
        for line in lines:
            try:
                line = line.decode('ascii')
            except UnicodeDecodeError:
                raise NetworkXError('input is not ASCII-encoded')
            if not isinstance(line, str):
                lines = str(lines)
            if line and line[-1] == '\n':
                line = line[:-1]
            yield line

    G = parse_gml_lines(filter_lines(path), label, destringizer)
    return G


def parse_gml(lines, label='label', destringizer=None):
    """Parse GML graph from a string or iterable.

    Parameters
    ----------
    lines : string or iterable of strings
       Data in GML format.

    label : string, optional
        If not None, the parsed nodes will be renamed according to node
        attributes indicated by `label`. Default value: 'label'.

    destringizer : callable, optional
        A `destringizer` that recovers values stored as strings in GML. If it
        cannot convert a string to a value, a `ValueError` is raised. Default
        value : None.

    Returns
    -------
    G : NetworkX graph
        The parsed graph.

    Raises
    ------
    NetworkXError
        If the input cannot be parsed.

    See Also
    --------
    write_gml, read_gml, literal_destringizer

    Notes
    -----
    This stores nested GML attributes as dictionaries in the NetworkX graph,
    node, and edge attribute structures.

    GML files are stored using a 7-bit ASCII encoding with any extended
    ASCII characters (iso8859-1) appearing as HTML character entities.
    Without specifying a `stringizer`/`destringizer`, the code is capable of
    handling `int`/`float`/`str`/`dict`/`list` data as required by the GML
    specification.  For other data types, you need to explicitly supply a
    `stringizer`/`destringizer`.

    For additional documentation on the GML file format, please see the
    `GML website <http://www.infosun.fim.uni-passau.de/Graphlet/GML/gml-tr.html>`_.

    See the module docstring :mod:`networkx.readwrite.gml` for more details.
    """
    def decode_line(line):
        if isinstance(line, bytes):
            try:
                line.decode('ascii')
            except UnicodeDecodeError:
                raise NetworkXError('input is not ASCII-encoded')
        if not isinstance(line, str):
            line = str(line)
        return line

    def filter_lines(lines):
        if isinstance(lines, (str, unicode)):
            lines = decode_line(lines)
            lines = lines.splitlines()
            for line in lines:
                yield line
        else:
            for line in lines:
                line = decode_line(line)
                if line and line[-1] == '\n':
                    line = line[:-1]
                if line.find('\n') != -1:
                    raise NetworkXError('input line contains newline')
                yield line

    G = parse_gml_lines(filter_lines(lines), label, destringizer)
    return G


def parse_gml_lines(lines, label, destringizer):
    """Parse GML `lines` into a graph.
    """
    def tokenize():
        patterns = [
            r'[A-Za-z][0-9A-Za-z_]*\b',  # keys
            r'[+-]?(?:[0-9]*\.[0-9]+|[0-9]+\.[0-9]*)(?:[Ee][+-]?[0-9]+)?',  # reals
            r'[+-]?[0-9]+',   # ints
            r'".*?"',         # strings
            r'\[',            # dict start
            r'\]',            # dict end
            r'#.*$|\s+'       # comments and whitespaces
        ]
        tokens = re.compile(
            '|'.join('(' + pattern + ')' for pattern in patterns))
        lineno = 0
        for line in lines:
            length = len(line)
            pos = 0
            while pos < length:
                match = tokens.match(line, pos)
                if match is not None:
                    for i in range(len(patterns)):
                        group = match.group(i + 1)
                        if group is not None:
                            if i == 0:    # keys
                                value = group.rstrip()
                            elif i == 1:  # reals
                                value = float(group)
                            elif i == 2:  # ints
                                value = int(group)
                            else:
                                value = group
                            if i != 6:    # comments and whitespaces
                                yield (i, value, lineno + 1, pos + 1)
                            pos += len(group)
                            break
                else:
                    raise NetworkXError('cannot tokenize %r at (%d, %d)' %
                                        (line[pos:], lineno + 1, pos + 1))
            lineno += 1
        yield (None, None, lineno + 1, 1)  # EOF

    def unexpected(curr_token, expected):
        category, value, lineno, pos = curr_token
        raise NetworkXError(
            'expected %s, found %s at (%d, %d)' %
            (expected, repr(value) if value is not None else 'EOF', lineno,
             pos))

    def consume(curr_token, category, expected):
        if curr_token[0] == category:
            return next(tokens)
        unexpected(curr_token, expected)

    def parse_kv(curr_token):
        dct = defaultdict(list)
        while curr_token[0] == 0:  # keys
            key = curr_token[1]
            curr_token = next(tokens)
            category = curr_token[0]
            if category == 1 or category == 2:  # reals or ints
                value = curr_token[1]
                curr_token = next(tokens)
            elif category == 3:  # strings
                value = unescape(curr_token[1][1:-1])
                if destringizer:
                    try:
                        value = destringizer(value)
                    except ValueError:
                        pass
                curr_token = next(tokens)
            elif category == 4:  # dict start
                curr_token, value = parse_dict(curr_token)
            else:
                unexpected(curr_token, "an int, float, string or '['")
            dct[key].append(value)
        dct = {key: (value if not isinstance(value, list) or len(value) != 1
                     else value[0]) for key, value in dct.items()}
        return curr_token, dct

    def parse_dict(curr_token):
        curr_token = consume(curr_token, 4, "'['")    # dict start
        curr_token, dct = parse_kv(curr_token)
        curr_token = consume(curr_token, 5, "']'")  # dict end
        return curr_token, dct

    def parse_graph():
        curr_token, dct = parse_kv(next(tokens))
        if curr_token[0] is not None:  # EOF
            unexpected(curr_token, 'EOF')
        if 'graph' not in dct:
            raise NetworkXError('input contains no graph')
        graph = dct['graph']
        if isinstance(graph, list):
            raise NetworkXError('input contains more than one graph')
        return graph

    tokens = tokenize()
    graph = parse_graph()

    directed = graph.pop('directed', False)
    multigraph = graph.pop('multigraph', False)
    if not multigraph:
        G = nx.DiGraph() if directed else nx.Graph()
    else:
        G = nx.MultiDiGraph() if directed else nx.MultiGraph()
    G.graph.update((key, value) for key, value in graph.items()
                   if key != 'node' and key != 'edge')

    def pop_attr(dct, category, attr, i):
        try:
            return dct.pop(attr)
        except KeyError:
            raise NetworkXError(
                "%s #%d has no '%s' attribute" % (category, i, attr))

    nodes = graph.get('node', [])
    mapping = {}
    node_labels = set()
    for i, node in enumerate(nodes if isinstance(nodes, list) else [nodes]):
        id = pop_attr(node, 'node', 'id', i)
        if id in G:
            raise NetworkXError('node id %r is duplicated' % (id,))
        if label is not None and label != 'id':
            node_label = pop_attr(node, 'node', label, i)
            if node_label in node_labels:
                raise NetworkXError('node label %r is duplicated' %
                                    (node_label,))
            node_labels.add(node_label)
            mapping[id] = node_label
        G.add_node(id, **node)

    edges = graph.get('edge', [])
    for i, edge in enumerate(edges if isinstance(edges, list) else [edges]):
        source = pop_attr(edge, 'edge', 'source', i)
        target = pop_attr(edge, 'edge', 'target', i)
        if source not in G:
            raise NetworkXError(
                'edge #%d has an undefined source %r' % (i, source))
        if target not in G:
            raise NetworkXError(
                'edge #%d has an undefined target %r' % (i, target))
        if not multigraph:
            if not G.has_edge(source, target):
                G.add_edge(source, target, **edge)
            else:
                raise nx.NetworkXError(
                    """edge #%d (%r%s%r) is duplicated

Hint:  If this is a multigraph, add "multigraph 1" to the header of the file.""" %
                    (i, source, '->' if directed else '--', target))
        else:
            key = edge.pop('key', None)
            if key is not None and G.has_edge(source, target, key):
                raise nx.NetworkXError(
                    'edge #%d (%r%s%r, %r) is duplicated' %
                    (i, source, '->' if directed else '--', target, key))
            G.add_edge(source, target, key, **edge)

    if label is not None and label != 'id':
        G = nx.relabel_nodes(G, mapping)
    return G


def literal_stringizer(value):
    """Convert a `value` to a Python literal in GML representation.

    Parameters
    ----------
    value : object
        The `value` to be converted to GML representation.

    Returns
    -------
    rep : string
        A double-quoted Python literal representing value. Unprintable
        characters are replaced by XML character references.

    Raises
    ------
    ValueError
        If `value` cannot be converted to GML.

    Notes
    -----
    `literal_stringizer` is largely the same as `repr` in terms of
    functionality but attempts prefix `unicode` and `bytes` literals with
    `u` and `b` to provide better interoperability of data generated by
    Python 2 and Python 3.

    The original value can be recovered using the
    :func:`networkx.readwrite.gml.literal_destringizer` function.
    """
    def stringize(value):
        if isinstance(value, (int, long, bool)) or value is None:
            if value is True:  # GML uses 1/0 for boolean values.
                buf.write(str(1))
            elif value is False:
                buf.write(str(0))
            else:
                buf.write(str(value))
        elif isinstance(value, unicode):
            text = repr(value)
            if text[0] != 'u':
                try:
                    value.encode('latin1')
                except UnicodeEncodeError:
                    text = 'u' + text
            buf.write(text)
        elif isinstance(value, (float, complex, str, bytes)):
            buf.write(repr(value))
        elif isinstance(value, list):
            buf.write('[')
            first = True
            for item in value:
                if not first:
                    buf.write(',')
                else:
                    first = False
                stringize(item)
            buf.write(']')
        elif isinstance(value, tuple):
            if len(value) > 1:
                buf.write('(')
                first = True
                for item in value:
                    if not first:
                        buf.write(',')
                    else:
                        first = False
                    stringize(item)
                buf.write(')')
            elif value:
                buf.write('(')
                stringize(value[0])
                buf.write(',)')
            else:
                buf.write('()')
        elif isinstance(value, dict):
            buf.write('{')
            first = True
            for key, value in value.items():
                if not first:
                    buf.write(',')
                else:
                    first = False
                stringize(key)
                buf.write(':')
                stringize(value)
            buf.write('}')
        elif isinstance(value, set):
            buf.write('{')
            first = True
            for item in value:
                if not first:
                    buf.write(',')
                else:
                    first = False
                stringize(item)
            buf.write('}')
        else:
            raise ValueError(
                '%r cannot be converted into a Python literal' % (value,))

    buf = StringIO()
    stringize(value)
    return buf.getvalue()


def generate_gml(G, stringizer=None):
    r"""Generate a single entry of the graph `G` in GML format.

    Parameters
    ----------
    G : NetworkX graph
        The graph to be converted to GML.

    stringizer : callable, optional
        A `stringizer` which converts non-int/non-float/non-dict values into
        strings. If it cannot convert a value into a string, it should raise a
        `ValueError` to indicate that. Default value: None.

    Returns
    -------
    lines: generator of strings
        Lines of GML data. Newlines are not appended.

    Raises
    ------
    NetworkXError
        If `stringizer` cannot convert a value into a string, or the value to
        convert is not a string while `stringizer` is None.

    See Also
    --------
    literal_stringizer

    Notes
    -----
    Graph attributes named 'directed', 'multigraph', 'node' or
    'edge', node attributes named 'id' or 'label', edge attributes
    named 'source' or 'target' (or 'key' if `G` is a multigraph)
    are ignored because these attribute names are used to encode the graph
    structure.

    GML files are stored using a 7-bit ASCII encoding with any extended
    ASCII characters (iso8859-1) appearing as HTML character entities.
    Without specifying a `stringizer`/`destringizer`, the code is capable of
    handling `int`/`float`/`str`/`dict`/`list` data as required by the GML
    specification.  For other data types, you need to explicitly supply a
    `stringizer`/`destringizer`.

    For additional documentation on the GML file format, please see the
    `GML website <http://www.infosun.fim.uni-passau.de/Graphlet/GML/gml-tr.html>`_.

    See the module docstring :mod:`networkx.readwrite.gml` for more details.

    Examples
    --------
    >>> G = nx.Graph()
    >>> G.add_node("1")
    >>> print("\n".join(nx.generate_gml(G)))
    graph [
      node [
        id 0
        label "1"
      ]
    ]
    >>> G = nx.OrderedMultiGraph([("a", "b"), ("a", "b")])
    >>> print("\n".join(nx.generate_gml(G)))
    graph [
      multigraph 1
      node [
        id 0
        label "a"
      ]
      node [
        id 1
        label "b"
      ]
      edge [
        source 0
        target 1
        key 0
      ]
      edge [
        source 0
        target 1
        key 1
      ]
    ]
    """
    valid_keys = re.compile('^[A-Za-z][0-9A-Za-z]*$')

    def stringize(key, value, ignored_keys, indent, in_list=False):
        if not isinstance(key, (str, unicode)):
            raise NetworkXError('%r is not a string' % (key,))
        if not valid_keys.match(key):
            raise NetworkXError('%r is not a valid key' % (key,))
        if not isinstance(key, str):
            key = str(key)
        if key not in ignored_keys:
            if isinstance(value, (int, long, bool)):
                if key == 'label':
                    yield indent + key + ' "' + str(value) + '"'
                elif value is True:
                    # python bool is an instance of int
                    yield indent + key + ' 1'
                elif value is False:
                    yield indent + key + ' 0'
                else:
                    yield indent + key + ' ' + str(value)
            elif isinstance(value, float):
                text = repr(value).upper()
                # GML requires that a real literal contain a decimal point, but
                # repr may not output a decimal point when the mantissa is
                # integral and hence needs fixing.
                epos = text.rfind('E')
                if epos != -1 and text.find('.', 0, epos) == -1:
                    text = text[:epos] + '.' + text[epos:]
                if key == 'label':
                    yield indent + key + ' "' + text + '"'
                else:
                    yield indent + key + ' ' + text
            elif isinstance(value, dict):
                yield indent + key + ' ['
                next_indent = indent + '  '
                for key, value in value.items():
                    for line in stringize(key, value, (), next_indent):
                        yield line
                yield indent + ']'
            elif isinstance(value, (list, tuple)) and key != 'label' \
                    and value and not in_list:
                next_indent = indent + '  '
                for val in value:
                    for line in stringize(key, val, (), next_indent, True):
                        yield line
            else:
                if stringizer:
                    try:
                        value = stringizer(value)
                    except ValueError:
                        raise NetworkXError(
                            '%r cannot be converted into a string' % (value,))
                if not isinstance(value, (str, unicode)):
                    raise NetworkXError('%r is not a string' % (value,))
                yield indent + key + ' "' + escape(value) + '"'

    multigraph = G.is_multigraph()
    yield 'graph ['

    # Output graph attributes
    if G.is_directed():
        yield '  directed 1'
    if multigraph:
        yield '  multigraph 1'
    ignored_keys = {'directed', 'multigraph', 'node', 'edge'}
    for attr, value in G.graph.items():
        for line in stringize(attr, value, ignored_keys, '  '):
            yield line

    # Output node data
    node_id = dict(zip(G, range(len(G))))
    ignored_keys = {'id', 'label'}
    for node, attrs in G.nodes.items():
        yield '  node ['
        yield '    id ' + str(node_id[node])
        for line in stringize('label', node, (), '    '):
            yield line
        for attr, value in attrs.items():
            for line in stringize(attr, value, ignored_keys, '    '):
                yield line
        yield '  ]'

    # Output edge data
    ignored_keys = {'source', 'target'}
    kwargs = {'data': True}
    if multigraph:
        ignored_keys.add('key')
        kwargs['keys'] = True
    for e in G.edges(**kwargs):
        yield '  edge ['
        yield '    source ' + str(node_id[e[0]])
        yield '    target ' + str(node_id[e[1]])
        if multigraph:
            for line in stringize('key', e[2], (), '    '):
                yield line
        for attr, value in e[-1].items():
            for line in stringize(attr, value, ignored_keys, '    '):
                yield line
        yield '  ]'
    yield ']'


@open_file(1, mode='wb')
def write_gml(G, path, stringizer=None):
    """Write a graph `G` in GML format to the file or file handle `path`.

    Parameters
    ----------
    G : NetworkX graph
        The graph to be converted to GML.

    path : filename or filehandle
        The filename or filehandle to write. Files whose names end with .gz or
        .bz2 will be compressed.

    stringizer : callable, optional
        A `stringizer` which converts non-int/non-float/non-dict values into
        strings. If it cannot convert a value into a string, it should raise a
        `ValueError` to indicate that. Default value: None.

    Raises
    ------
    NetworkXError
        If `stringizer` cannot convert a value into a string, or the value to
        convert is not a string while `stringizer` is None.

    See Also
    --------
    read_gml, generate_gml, literal_stringizer

    Notes
    -----
    Graph attributes named 'directed', 'multigraph', 'node' or
    'edge', node attributes named 'id' or 'label', edge attributes
    named 'source' or 'target' (or 'key' if `G` is a multigraph)
    are ignored because these attribute names are used to encode the graph
    structure.

    GML files are stored using a 7-bit ASCII encoding with any extended
    ASCII characters (iso8859-1) appearing as HTML character entities.
    Without specifying a `stringizer`/`destringizer`, the code is capable of
    handling `int`/`float`/`str`/`dict`/`list` data as required by the GML
    specification.  For other data types, you need to explicitly supply a
    `stringizer`/`destringizer`.

    Note that while we allow non-standard GML to be read from a file, we make
    sure to write GML format. In particular, underscores are not allowed in
    attribute names.
    For additional documentation on the GML file format, please see the
    `GML website <http://www.infosun.fim.uni-passau.de/Graphlet/GML/gml-tr.html>`_.

    See the module docstring :mod:`networkx.readwrite.gml` for more details.

    Examples
    --------
    >>> G = nx.path_graph(4)
    >>> nx.write_gml(G, "test.gml")

    Filenames ending in .gz or .bz2 will be compressed.

    >>> nx.write_gml(G, "test.gml.gz")
    """
    for line in generate_gml(G, stringizer):
        path.write((line + '\n').encode('ascii'))


# fixture for nose
def teardown_module(module):
    import os
    for fname in ['test.gml', 'test.gml.gz']:
        if os.path.isfile(fname):
            os.unlink(fname)