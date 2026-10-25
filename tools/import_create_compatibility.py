"""Build optional native IC2 processing recipes from installed Create and native recipe data."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
parser = argparse.ArgumentParser()
parser.add_argument('create_jar', type=Path)
args = parser.parse_args()
condition = [{'type': 'neoforge:mod_loaded', 'modid': 'create'}]

def write(path, data):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', 'utf-8')

def processing(kind, name, ingredient, results, **metadata):
    write(f'data/ic2/recipe/{kind}/compat/create/{name}.json', {
        'neoforge:conditions': condition, 'type': 'ic2:' + kind,
        'ingredient': ingredient, 'result': results, **metadata})

def smelt(name, ingredient, output, experience):
    write(f'data/ic2/recipe/smelting/compat/create/{name}.json', {
        'neoforge:conditions': condition, 'type': 'minecraft:smelting',
        'group': 'ic2_create_processing', 'category': 'misc',
        'ingredient': ingredient, 'result': {'id': output},
        'experience': experience, 'cookingtime': 200})

write('packs/create_compat/pack.mcmeta', {'pack': {'pack_format': 48, 'description': {'translate': 'ic2.pack.create_compat'}}})

with zipfile.ZipFile(args.create_jar) as archive:
    assert 'assets/create/models/item/crushed_raw_zinc.json' in archive.namelist()
    for metal in ['copper', 'iron', 'gold', 'lead', 'tin', 'silver', 'uranium']:
        replacement = 'create:crushed_raw_' + metal
        assert 'assets/create/models/item/crushed_raw_' + metal + '.json' in archive.namelist()
        for kind in ['macerator', 'ore_washer', 'centrifuge', 'smelting']:
            folder = ROOT / f'data/ic2/recipe/{kind}'
            for file in folder.glob('*.json'):
                original = json.loads(file.read_text('utf-8'))
                if original.get('ingredient', {}).get('item') != 'ic2:crushed_' + metal:
                    continue
                copied = json.loads(json.dumps(original))
                copied['ingredient'] = {'item': replacement}
                copied['neoforge:conditions'] = condition
                if kind == 'smelting':
                    copied['group'] = 'ic2_create_processing'
                    name = metal + '_ingot_from_crushed'
                else:
                    name = file.stem
                write(f'data/ic2/recipe/{kind}/compat/create/{name}.json', copied)

    # Create targets older IC2 registry names. Override only those broken compat
    # recipes; absent aluminium remains absent instead of adding a Classic chain.
    for name in archive.namelist():
        if not name.startswith('data/create/recipe/') or not name.endswith('.json'): continue
        raw = archive.read(name).decode('utf8')
        if 'ic2:' not in raw: continue
        data = json.loads(raw)
        aluminium = 'aluminium' in raw
        data['neoforge:conditions'] = [{'type':'neoforge:mod_loaded','modid':'create'}]
        if aluminium:
            data['neoforge:conditions'].append({'type':'neoforge:item_exists','item':'ic2:aluminium_ingot'})
        else:
            for metal in ['tin','silver','uranium']:
                if 'ic2:ingot_' + metal in raw:
                    data['result']['id'] = 'ic2:' + metal + '_ingot'
                    data['experience'] = .25
                if 'ic2:nugget_' + metal in raw:
                    data['results'] = [{'id':'ic2:' + metal + '_ingot','count':1}]
        write('packs/create_compat/' + name, data)

    processing('macerator', 'raw_zinc_to_crushed', {'tag': 'c:raw_materials/zinc'}, {'item': 'create:crushed_raw_zinc', 'count': 2})
    processing('macerator', 'zinc_ore_to_crushed', {'tag': 'c:ores/zinc'}, {'item': 'create:crushed_raw_zinc', 'count': 2})
    processing('macerator', 'raw_zinc_block_to_crushed', {'tag': 'c:storage_blocks/raw_zinc'}, {'item': 'create:crushed_raw_zinc', 'count': 18})
    for metal, dust, small, purified, ingot in [
        ('zinc', 'ic2:zinc_dust', 'ic2:small_zinc_dust', 'ic2:purified_zinc', 'create:zinc_ingot')]:
        processing('macerator', metal + '_crushed_to_dust', {'item': 'create:crushed_raw_' + metal}, {'item': dust})
        processing('macerator', metal + '_purified_to_dust', {'item': purified}, {'item': dust})
        processing('macerator', metal + '_ingot_to_dust', {'item': ingot}, {'item': dust})
        processing('ore_washer', metal + '_washing', {'item': 'create:crushed_raw_' + metal},
                   [{'item': purified}, {'item': small, 'count': 2}, {'item': 'ic2:stone_dust'}], amount=1000)
        processing('centrifuge', metal + '_centrifuging', {'item': 'create:crushed_raw_' + metal},
                   [{'item': dust}, {'item': small}, {'item': 'ic2:stone_dust'}], minHeat=500)
        processing('centrifuge', metal + '_purified_centrifuging', {'item': purified},
                   [{'item': dust}, {'item': small}], minHeat=500)
        smelt(metal + '_ingot_from_crushed', {'item': 'create:crushed_raw_' + metal}, ingot, .35)
        smelt(metal + '_ingot_from_purified', {'item': purified}, ingot, .35)
        if metal == 'zinc': smelt('zinc_ingot_from_dust', {'item': dust}, ingot, .35)
        write(f'data/ic2/recipe/shaped/compat/create/{metal}_dust_from_small.json', {
            'neoforge:conditions': condition, 'type': 'minecraft:crafting_shaped',
            'pattern': ['XXX', 'XXX', 'XXX'], 'key': {'X': {'item': small}}, 'result': {'id': dust}})

    for name, original in [('purified_zinc', 'purified_silver'), ('zinc_dust', 'silver_dust'),
                           ('small_zinc_dust', 'small_silver_dust')]:
        source = ROOT / f'assets/ic2/models/item/{original}.json'
        write(f'assets/ic2/models/item/{name}.json', json.loads(source.read_text('utf-8')))
    for material, values in [('zinc', ['ic2:zinc_dust'])]:
        write(f'data/c/tags/item/dusts/{material}.json', {'replace': False, 'values': [{'id': v, 'required': False} for v in values]})

names = {
    'purified_zinc': ('Purified Crushed Zinc Ore', 'Очищенная дроблёная цинковая руда'),
    'zinc_dust': ('Zinc Dust', 'Цинковая пыль'),
    'small_zinc_dust': ('Tiny Pile of Zinc Dust', 'Маленькая кучка цинковой пыли')}
for index, language in enumerate(['en_us', 'ru_ru']):
    path = ROOT / f'assets/ic2/lang/{language}.json'
    data = json.loads(path.read_text('utf-8'))
    for name, labels in names.items(): data['item.ic2.' + name] = labels[index]
    write(path.relative_to(ROOT), data)

manifest = {'reference_jar': args.create_jar.name, 'reference_sha256': hashlib.sha256(args.create_jar.read_bytes()).hexdigest(),
            'optional': True, 'reuses_create_crushed_items': True,
            'native_metals': ['copper', 'iron', 'gold', 'lead', 'tin', 'silver', 'uranium', 'zinc'],
            'xp': 'Data-driven native processing rewards; Create original recipes remain available to other furnaces.'}
(ROOT.parents[2] / 'docs/create-processing-compatibility.json').write_text(json.dumps(manifest, indent=2) + '\n', 'utf-8')
