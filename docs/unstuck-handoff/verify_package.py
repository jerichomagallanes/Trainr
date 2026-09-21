#!/usr/bin/env python3
'''Portable handoff integrity checks. Not a production or complete JSON Schema validator.'''
import hashlib, json, re, struct
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit, unquote
ROOT = Path(__file__).resolve().parent

def check(schema, value):
    if 'anyOf' in schema:
        for option in schema['anyOf']:
            try:
                check(option, value)
                return
            except AssertionError:
                pass
        raise AssertionError('No anyOf branch matches')
    if 'const' in schema: assert value == schema['const']
    if 'enum' in schema: assert value in schema['enum']
    if 'type' in schema:
        types = schema['type'] if isinstance(schema['type'], list) else [schema['type']]
        checks = {'null': value is None, 'boolean': type(value) is bool,
                  'integer': type(value) is int, 'number': type(value) in (int, float),
                  'string': isinstance(value, str), 'object': isinstance(value, dict),
                  'array': isinstance(value, list)}
        assert any(checks[t] for t in types), (types, value)
    if isinstance(value, dict):
        assert set(schema.get('required', [])) <= set(value)
        props = schema.get('properties', {})
        if schema.get('additionalProperties') is False: assert set(value) <= set(props)
        for key, item in value.items():
            if key in props: check(props[key], item)
    if isinstance(value, list):
        assert len(value) >= schema.get('minItems', 0)
        assert len(value) <= schema.get('maxItems', float('inf'))
        if schema.get('uniqueItems'): assert len({json.dumps(v, sort_keys=True) for v in value}) == len(value)
        for item in value: check(schema.get('items', {}), item)
    if isinstance(value, str):
        assert len(value) >= schema.get('minLength', 0)
        assert len(value) <= schema.get('maxLength', float('inf'))
    if type(value) in (int, float):
        assert value >= schema.get('minimum', float('-inf'))
        assert value <= schema.get('maximum', float('inf'))

class Links(HTMLParser):
    def __init__(self): super().__init__(); self.refs = []
    def handle_starttag(self, tag, attrs):
        self.refs.extend(v for k, v in attrs if k in ('href', 'src') and v)

json_files = list(ROOT.rglob('*.json'))
for path in json_files: json.loads(path.read_text())
for path in ROOT.rglob('*'):
    if path.suffix not in ('.md', '.html'): continue
    text = path.read_text()
    if path.suffix == '.md': refs = re.findall(r'\]\(([^)]+)\)', text)
    else:
        parser = Links(); parser.feed(text); refs = parser.refs
    for ref in refs:
        url = urlsplit(ref.strip('<>'))
        if url.scheme or not url.path: continue
        assert (path.parent / unquote(url.path)).exists(), (path, ref)

scenes = json.loads((ROOT/'prototype/scenes.json').read_text())
assert len({s['id'] for s in scenes}) == len(scenes)
fixture_source = (ROOT/'prototype/fixtures.js').read_text()
assert json.loads(re.search(r'const captureScenes = (.*);', fixture_source).group(1)) == scenes
for scene in scenes:
    for theme in ('light', 'dark'):
        raw = (ROOT/'screenshots'/f'{scene["id"]}-{theme}.png').read_bytes()
        assert raw[:8] == b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II', raw[16:24]) == (390, 844)
intent_schema = json.loads((ROOT/'contracts/intent.schema.json').read_text())
valid = json.loads((ROOT/'fixtures/intent-valid.json').read_text())
check(intent_schema, valid['output'])
for span in valid['output']['evidence']:
    assert 0 <= span['start'] < span['end'] <= len(valid['input'])
    assert valid['input'][span['start']:span['end']] == span['quote']
invalid = json.loads((ROOT/'fixtures/intent-invalid-extra-key.json').read_text())
try: check(intent_schema, invalid['output'])
except AssertionError: pass
else: raise AssertionError('Invalid extra key accepted')
check(json.loads((ROOT/'contracts/proposal.schema.json').read_text()),
      json.loads((ROOT/'fixtures/proposal-example.json').read_text()))
manifest = ROOT/'SHA256SUMS'
if manifest.exists():
    for line in manifest.read_text().splitlines():
        digest, relative = line.split('  ', 1)
        assert hashlib.sha256((ROOT/relative).read_bytes()).hexdigest() == digest, relative
print(f'PASS: {len(json_files)} JSON files; local document/HTML links; {len(scenes)*2} 390x844 references; fixture shape/evidence checks; available hashes.')
print('Scope: lightweight checks for schema vocabulary used here. No native app, model, clinical, full JSON Schema conformance or migration tests run.')
