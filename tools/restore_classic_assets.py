"""Reproduce restored Classic assets, material recipes and model geometry.

Original images are copied without editing. Supply the documented original JAR.
"""
import hashlib
import json
import struct
import zipfile
from pathlib import Path

repo = Path(__file__).resolve().parents[1]
root = repo / 'src/main/resources'
import argparse
parser = argparse.ArgumentParser(description='Restore original Classic assets and material data.')
parser.add_argument('archive', type=Path, help='Original IC2Classic 1.19.2-2.1.3.4 JAR')
archive = parser.parse_args().archive
expected = 'f0a99733ef33491eca418831d681a79902250d341650f652d06f8b88d3627f83'
if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
    raise SystemExit('The input does not match the documented original Classic release.')
manifest_path = repo / 'docs/classic-assets-21.1.70.json'
manifest = json.loads(manifest_path.read_text('utf-8')) if manifest_path.exists() else {}

def write(path, value):
    p = root / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def tag(kind, name, values):
    p = root / f'data/{name.split(":")[0]}/tags/{kind}/{name.split(":")[1]}.json'
    data = json.loads(p.read_text('utf-8')) if p.exists() else {'replace': False, 'values': []}
    for v in values:
        if v not in data['values']: data['values'].append(v)
    write(p.relative_to(root), data)

def asset(z, source):
    p = root / source
    data = z.read(source)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(data)
    manifest[source] = {'source': 'IC2Classic-1.19.2-2.1.3.4.jar', 'source_path': source,
                        'sha256': hashlib.sha256(data).hexdigest(),
                        'size': list(struct.unpack('>II', data[16:24]))}
    return 'ic2:' + source.removeprefix('assets/ic2/textures/').removesuffix('.png')

def item_model(name, texture):
    write(f'assets/ic2/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': texture}})

def cube(name, texture):
    write(f'assets/ic2/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}})
    write(f'assets/ic2/models/item/{name}.json', {'parent': f'ic2:block/{name}'})
    write(f'assets/ic2/blockstates/{name}.json', {'variants': {'': {'model': f'ic2:block/{name}'}}})

def shaped(name, result, pattern, keys, count=1):
    write(f'data/ic2/recipe/shaped/{name}.json', {'type': 'minecraft:crafting_shaped', 'pattern': pattern,
          'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v}) for k,v in keys.items()},
          'result': {'id': result, 'count': count}})

def machine(kind, name, input, output, count=1, input_count=1):
    ingredient = {'tag': input[1:]} if input.startswith('#') else {'item': input}
    if input_count != 1: ingredient['count'] = input_count
    result = {'item': output}
    if count != 1: result['count'] = count
    write(f'data/ic2/recipe/{kind}/{name}.json', {'type': f'ic2:{kind}', 'ingredient': ingredient, 'result': result})

def self_drop(name):
    write(f'data/ic2/loot_table/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'ic2:{name}'}], 'conditions':[{'condition':'minecraft:survives_explosion'}]}]})

with zipfile.ZipFile(archive) as z:
    for name in ['silver_ore','deepslate_silver_ore','silver_ore_nether','aluminium_ore_nether']:
        cube(name, asset(z, f'assets/ic2/textures/block/resources/ore/{name}.png'))
        raw = 'aluminium' if 'aluminium' in name else 'silver'
        minimum, maximum = (15,25) if raw == 'aluminium' else ((2,5) if 'nether' in name else (1,3))
        silk = {'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}
        write(f'data/ic2/loot_table/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
            {'type':'minecraft:item','conditions':[silk],'name':f'ic2:{name}'},
            {'type':'minecraft:item','name':f'ic2:raw_{raw}','functions':[
                {'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':minimum,'max':maximum}},
                {'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'},
                {'function':'minecraft:explosion_decay'}]}]}]}]})
        for kind in ['item','block']:
            for t in [f'c:ores/{raw}', 'c:ores', 'minecraft:mineable/pickaxe' if kind == 'block' else None,
                      'minecraft:needs_iron_tool' if kind == 'block' else None,
                      'c:ores_in_ground/netherrack' if 'nether' in name else 'c:ores_in_ground/deepslate' if 'deepslate' in name else 'c:ores_in_ground/stone']:
                if t: tag(kind,t,[f'ic2:{name}'])
    textures = {'raw_silver':'raw/silver','raw_aluminium':'raw/aluminium', 'aluminium_dust':'dust/aluminium',
        'aluminium_ingot':'ingots/aluminium','refined_iron_ingot':'ingots/refined_iron','rare_earth_dust':'dust/rare_earth',
        'rare_earth_chunk':'misc/rare_earth_chunk','dead_magnet':'misc/dead_magnet','magnet':'misc/magnet',
        'pulsating_quartz':'misc/pulsating_quartz','complex_circuit':'misc/complex_circuit','turbine_blade':'blades/turbine_blade','plasma_core':'misc/plasma_core'}
    for name, path in textures.items(): item_model(name, asset(z, f'assets/ic2/textures/item/{path}.png'))
    for name, path in {'raw_silver_block':'resources/raw/raw_silver_block','raw_aluminium_block':'resources/raw/raw_aluminium_block',
        'aluminium_block':'resources/storage/aluminium_block','refined_iron_block':'resources/storage/refined_iron_block','iridium_stone':'personal/iridium_stone'}.items():
        cube(name, asset(z, f'assets/ic2/textures/block/{path}.png'))
        self_drop(name)
    cube('colossal_base_block', asset(z, 'assets/ic2/textures/block/misc/colossal_base/all.png'))
    self_drop('colossal_base_block')
    for name, path in {'stabilized_machine_block':'misc/stabilized_machine_block'}.items():
        faces = {d: asset(z, f'assets/ic2/textures/block/{path}/{d}.png') for d in ['down','up','north','south','west','east']}
        write(f'assets/ic2/models/block/{name}.json',{'parent':'minecraft:block/cube','textures':dict(faces,particle=faces['north'])})
        write(f'assets/ic2/models/item/{name}.json',{'parent':f'ic2:block/{name}'})
        write(f'assets/ic2/blockstates/{name}.json',{'variants':{'':{'model':f'ic2:block/{name}'}}})
        self_drop(name)

for name, metal in [('raw_silver','silver'),('raw_aluminium','aluminium')]:
    tag('item',f'c:raw_materials/{metal}',[f'ic2:{name}'])
    tag('item','c:raw_materials',[f'ic2:{name}'])
    shaped(name + '_block',f'ic2:raw_{metal}_block',['XXX']*3,{'X':f'#c:raw_materials/{metal}'})
    write(f'data/ic2/recipe/shapeless/{name}_from_block.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':f'ic2:raw_{metal}_block'}],'result':{'id':f'ic2:{name}','count':9}})
    machine('macerator',name + '_to_dust',f'#c:raw_materials/{metal}',f'ic2:{metal}_dust',2)
machine('macerator','aluminium_ore_to_dust','#c:ores/aluminium','ic2:aluminium_dust',2)
for metal in ['aluminium','refined_iron']:
    tag('item',f'c:ingots/{metal}',[f'ic2:{metal}_ingot'])
    tag('item','c:ingots',[f'ic2:{metal}_ingot'])
    shaped(metal + '_block',f'ic2:{metal}_block',['XXX']*3,{'X':f'#c:ingots/{metal}'})
    write(f'data/ic2/recipe/shapeless/{metal}_ingots_from_block.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':f'ic2:{metal}_block'}],'result':{'id':f'ic2:{metal}_ingot','count':9}})
tag('item','c:dusts/aluminium',['ic2:aluminium_dust'])
tag('item','c:dusts',['ic2:aluminium_dust'])
machine('compressor','rare_earth_chunk_to_dead_magnet','ic2:rare_earth_chunk','ic2:dead_magnet')
shaped('rare_earth_chunk','ic2:rare_earth_chunk',[' X ','XYX',' X '],{'X':'ic2:rare_earth_dust','Y':'#c:ingots/iron'})
for form in ['raw_silver','silver_ore','deepslate_silver_ore','silver_ore_nether']:
    write(f'data/ic2/recipe/smelting/silver_ingot_from_{form}.json',{'type':'minecraft:smelting','category':'misc','ingredient':{'item':f'ic2:{form}'},'result':{'id':'ic2:silver_ingot'},'experience':0.8,'cookingtime':200})

targets = [{'target':{'predicate_type':'minecraft:tag_match','tag':'minecraft:stone_ore_replaceables'},'state':{'Name':'ic2:silver_ore'}},
           {'target':{'predicate_type':'minecraft:tag_match','tag':'minecraft:deepslate_ore_replaceables'},'state':{'Name':'ic2:deepslate_silver_ore'}}]
for name,size,chance,count,low,high,distribution in [('silver',5,0,10,-64,22,'trapezoid'),('silver_nether',3,0.1,10,0,128,'uniform'),('aluminium_nether',3,0.5,10,0,128,'uniform')]:
    state_targets = targets if name == 'silver' else [{'target':{'predicate_type':'minecraft:tag_match','tag':'minecraft:base_stone_nether'},'state':{'Name':f'ic2:{name.split("_")[0]}_ore_nether'}}]
    write(f'data/ic2/worldgen/configured_feature/ore_{name}.json',{'type':'minecraft:ore','config':{'size':size,'discard_chance_on_air_exposure':chance,'targets':state_targets}})
    write(f'data/ic2/worldgen/placed_feature/ore_{name}.json',{'feature':f'ic2:ore_{name}','placement':[
        {'type':'minecraft:count','count':count},{'type':'minecraft:in_square'},
        {'type':'minecraft:height_range','height':{'type':f'minecraft:{distribution}','min_inclusive':{'absolute':low},'max_inclusive':{'absolute':high}}},
        {'type':'minecraft:biome'}]})
for group,biomes,features in [('silver','#minecraft:is_overworld',['ic2:ore_silver']),('nether','#minecraft:is_nether',['ic2:ore_silver_nether','ic2:ore_aluminium_nether'])]:
    write(f'data/ic2/neoforge/biome_modifier/add_classic_ores_{group}.json',{'type':'neoforge:add_features','biomes':biomes,'features':features,'step':'underground_ores'})

names = {'silver_ore':('Silver Ore','Серебряная руда'),'deepslate_silver_ore':('Deepslate Silver Ore','Глубинная серебряная руда'),
 'silver_ore_nether':('Nether Silver Ore','Серебряная руда Незера'),'aluminium_ore_nether':('Nether Aluminium Ore','Алюминиевая руда Незера'),
 'raw_silver_block':('Block of Raw Silver','Блок рудного серебра'),'raw_aluminium_block':('Block of Raw Aluminium','Блок рудного алюминия'),
 'aluminium_block':('Block of Aluminium','Алюминиевый блок'),'refined_iron_block':('Block of Refined Iron','Блок очищенного железа'),
 'stabilized_machine_block':('Stabilized Machine Block','Стабилизированный корпус машины'),'colossal_base_block':('Colossal Machine Casing','Корпус колоссальной машины'),
 'iridium_stone':('Iridium Stone','Иридиевый камень'),'raw_silver':('Raw Silver','Рудное серебро'),'raw_aluminium':('Raw Aluminium','Рудный алюминий'),
 'aluminium_dust':('Aluminium Dust','Алюминиевая пыль'),'aluminium_ingot':('Aluminium Ingot','Алюминиевый слиток'),
 'refined_iron_ingot':('Refined Iron Ingot','Слиток очищенного железа'),'rare_earth_dust':('Rare Earth Dust','Редкоземельная пыль'),
 'rare_earth_chunk':('Rare Earth Chunk','Редкоземельный комок'),'dead_magnet':('Uncharged Magnet','Незаряженный магнит'),
 'magnet':('Magnet','Магнит'),'pulsating_quartz':('Pulsating Quartz','Пульсирующий кварц'),'complex_circuit':('Complex Circuit','Комплексная электросхема'),
 'turbine_blade':('Turbine Blade','Лопасть турбины'),'plasma_core':('Plasma Core','Плазменное ядро')}
blocks = set(names) - set(textures)
for i,locale in enumerate(['en_us','ru_ru']):
    path = root / f'assets/ic2/lang/{locale}.json'
    d=json.loads(path.read_text('utf-8'))
    for name,values in names.items(): d[f'{"block" if name in blocks else "item"}.ic2.{name}'] = values[i]
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest_path.write_text(json.dumps(dict(sorted(manifest.items())),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Imported {len(manifest)} original assets; added ore resources, loot, tags and recipes.')

with zipfile.ZipFile(archive) as z:
    item_model('solar_helmet', asset(z,'assets/ic2/textures/item/armor/solar_helmet/base.png'))
    item_model('static_boots', asset(z,'assets/ic2/textures/item/armor/hazmat/rubber_boots.png'))
    for source,target in [('solar_1','classic_solar_layer_1'),('rubber_1','classic_static_layer_1')]:
        original = f'assets/ic2/textures/models/armor/{source}.png'
        asset(z, original)
        destination = f'assets/ic2/textures/models/armor/{target}.png'
        (root/destination).write_bytes(z.read(original))
        manifest[destination] = dict(manifest[original])
for name,pattern,key in [
 ('solar_helmet',['III','ISI','CCC'],{'I':'#c:ingots/iron','S':'ic2:solar_generator','C':'ic2:insulated_copper_cable'}),
 ('static_boots',['I I','ISI','CCC'],{'I':'#c:ingots/iron','S':'#minecraft:wool','C':'ic2:insulated_copper_cable'})]:
    shaped(name,f'ic2:{name}',pattern,key)
for name,helmet in [('solar_helmet',True),('static_boots',False)]:
    shaped(name+'_from_armor',f'ic2:{name}',[' H ',' S ','CCC'],{'H':'minecraft:iron_helmet' if helmet else 'minecraft:iron_boots',
        'S':'ic2:solar_generator' if helmet else '#minecraft:wool','C':'ic2:insulated_copper_cable'})
for i,locale in enumerate(['en_us','ru_ru']):
    path=root/f'assets/ic2/lang/{locale}.json'
    d=json.loads(path.read_text('utf-8'))
    d.update({k:v[i] for k,v in {
        'item.ic2.solar_helmet':('Solar Helmet','Солнечный шлем'),
        'item.ic2.static_boots':('Static Boots','Статические ботинки'),
        'tooltip.ic2.solar_helmet.production':('Charges worn electric armor at 1 EU/t in sunlight.','Заряжает надетую электрическую броню на 1 EU/т при солнечном свете.'),
        'tooltip.ic2.static_boots.production':('Generates EU while walking; wool increases production.','Вырабатывают EU при ходьбе; шерсть увеличивает выработку.')}.items()})
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest_path.write_text(json.dumps(dict(sorted(manifest.items())),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Restored basic charging armor and original item/worn artwork.')


with zipfile.ZipFile(archive) as z:
 for name,tier in [('rare_earth_extractor','lv'),('centrifugal_rare_earth_extractor','mv'),('alloy_smelter','lv')]:
    for active in [False,True]:
        prefix='active' if active else 'inactive'
        faces={d:asset(z,f'assets/ic2/textures/block/machine/{tier}/{name}/{prefix}_{d}.png') for d in ['up','down','north','south','east','west']}
        write(f'assets/ic2/models/block/{name}_{prefix}.json',{'parent':'minecraft:block/cube','textures':dict(faces,particle=faces['north'])})
    variants={f'active={str(active).lower()},facing={facing}':dict({'model':f'ic2:block/{name}_{"active" if active else "inactive"}'},**({'y':rotation} if rotation else {})) for active in [False,True] for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]}
    write(f'assets/ic2/blockstates/{name}.json',{'variants':variants})
    write(f'assets/ic2/models/item/{name}.json',{'parent':f'ic2:block/{name}_inactive'})
    self_drop(name)
    tag('block','minecraft:mineable/pickaxe',[f'ic2:{name}'])
    tag('block','minecraft:needs_stone_tool',[f'ic2:{name}'])
    gui_name='rare_earth_centrifuge' if name.startswith('centrifugal') else name
    gui_path=f'assets/ic2/textures/gui_sprites/blocks/machines/{tier}/gui_{gui_name}.png'
    asset(z,gui_path)
    xml=['<?xml version="1.0" encoding="UTF-8"?>','<gui width="176" height="166">',
        f'  <image x="0" y="0" width="176" height="166" src="ic2:{gui_path.removeprefix("assets/ic2/")}" u="0" v="0" u2="176" v2="166"/>',
        '  <text y="3" align="center">%name%</text>']
    if name=='alloy_smelter':
        xml+=['  <slot name="input" index="0" x="46" y="17" style="plain"/>','  <slot name="input" index="1" x="66" y="17" style="plain"/>']
    else: xml+=['  <slot name="input" x="56" y="17" style="plain"/>']
    xml+=['  <slot name="output" x="116" y="35" style="plain"/>','  <slot name="discharge" x="56" y="53" style="plain"/>',
        '  <energygauge x="56" y="36" style="classicbolt"/>','  <gauge name="progress" x="79" y="34" style="classicprogress"/>']
    if name!='alloy_smelter':
        xml+=['  <tooltip text="{tooltip.ic2.classic.material_progress,%base.getMaterialPercent()%}"><gauge name="material" x="7" y="12" style="classicmaterial"/></tooltip>']
    if name.startswith('centrifugal'):
        xml+=['  <text x="5" y="36">{gui.ic2.classic.speed_label}</text>',
              '  <text x="5" y="44">%base.getSpeedPercent()%</text>']
    if name=='alloy_smelter':
        xml += [f'  <slot name="upgrade" index="{i}" x="152" y="{8+i*18}" style="plain"/>' for i in range(4)]
    else:
        xml += [f'  <slot name="upgrade" index="{i}" x="153" y="{19+i*18}" style="plain"/>' for i in range(2)]
    xml+=['  <playerInventory x="8" y="84" spacing="2" style="plain"/>','</gui>']
    path=root/f'assets/ic2/guidef/{name}.xml'
    path.write_text('\n'.join(xml)+'\n','utf-8')

def recipe(name,machine,inputs,result,count=1,units=16000,ticks=250,eu=16):
    write(f'data/ic2/recipe/classic_processing/{name}.json',{'type':'ic2:classic_processing','machine':machine,
        'inputs':[{'ingredient':{'tag':v[1:]} if v.startswith('#') else {'item':v},'count':n} for v,n in inputs],
        'result':{'id':result,'count':count},'ticks':ticks,'eu_per_tick':eu,'material_units':units})

sources=[(50,['stone','deepslate']),(1000,['granite','diorite','andesite','tuff']),(125,['netherrack']),
         (1600,['mycelium']),(3200,['obsidian','soul_sand','soul_soil'])]
for units,items in sources:
 for item in items: recipe('rare_earth_from_'+item,'rare_earth_extractor',[(f'minecraft:{item}',1)],'ic2:rare_earth_dust',units=units,ticks=35,eu=1)
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
for item in ['clay_ball','terracotta']+[c+'_terracotta' for c in colors]:
    recipe('aluminium_from_'+item,'rare_earth_extractor',[(f'minecraft:{item}',1)],'ic2:aluminium_dust',units=8000,ticks=35,eu=1)
recipe('aluminium_from_dirt','rare_earth_extractor',[('minecraft:dirt',1)],'ic2:aluminium_dust',units=1600,ticks=35,eu=1)
recipe('aluminium_smelting','alloy_smelter',[('#c:dusts/aluminium',1),('#minecraft:sand',1)],'ic2:aluminium_ingot')
recipe('pulsating_quartz','alloy_smelter',[('minecraft:redstone',2),('minecraft:quartz',1)],'ic2:pulsating_quartz')
recipe('netherite_ingot','alloy_smelter',[('minecraft:netherite_scrap',2),('minecraft:gold_ingot',2)],'minecraft:netherite_ingot')
for form,count,out in [('ores',3,8),('dusts',3,4),('ingots',3,4)]:
    recipe('bronze_from_'+form,'alloy_smelter',[(f'#c:{form}/copper',count),(f'#c:{form}/tin',1)],'ic2:bronze_ingot',out)
recipe('bronze_from_raw','alloy_smelter',[('minecraft:raw_copper',3),('ic2:raw_tin',1)],'ic2:bronze_ingot',8)
recipe('bronze_from_raw_copper','alloy_smelter',[('minecraft:raw_copper',3),('#c:ores/tin',1)],'ic2:bronze_ingot',8)
recipe('bronze_from_raw_tin','alloy_smelter',[('#c:ores/copper',3),('ic2:raw_tin',1)],'ic2:bronze_ingot',8)
recipe('bronze_from_blocks','alloy_smelter',[('#c:storage_blocks/copper',3),('#c:storage_blocks/tin',1)],'ic2:bronze_block',4)
recipe('bronze_from_mixed','alloy_smelter',[('#c:storage_blocks/copper',1),('#c:ingots/tin',3)],'ic2:bronze_ingot',12)
recipe('bronze_from_mixed_2','alloy_smelter',[('#c:ingots/copper',27),('#c:storage_blocks/tin',1)],'ic2:bronze_ingot',36)
shaped('rare_earth_extractor','ic2:rare_earth_extractor',['XYX','XCX'],{'X':'minecraft:glowstone_dust','Y':'ic2:extractor','C':'ic2:recycler'})
shaped('alloy_smelter','ic2:alloy_smelter',['XYX','XCX'],{'X':'minecraft:bricks','Y':'ic2:advanced_circuit','C':'ic2:electric_furnace'})
shaped('centrifugal_rare_earth_extractor','ic2:centrifugal_rare_earth_extractor',['XXX','XYX','XCX'],{'X':'ic2:electrolyzed_water_cell','C':'ic2:advanced_machine','Y':'ic2:rare_earth_extractor'})
centrifuge_path=root/'data/ic2/recipe/shaped/centrifugal_rare_earth_extractor.json'
centrifuge_recipe=json.loads(centrifuge_path.read_text('utf-8'))
centrifuge_recipe['neoforge:conditions']=[{'type':'neoforge:item_exists','item':'ic2:electrolyzed_water_cell'}]
write(centrifuge_path.relative_to(root),centrifuge_recipe)
write('data/ic2/recipe/smelting/iron_refining.json',{'type':'minecraft:smelting','category':'misc','ingredient':{'tag':'c:ingots/iron'},'result':{'id':'ic2:refined_iron_ingot'},'experience':0,'cookingtime':200})
shaped('stabilized_machine_block','ic2:stabilized_machine_block',[' C ','XYX',' C '],{'C':'ic2:magnet','Y':'ic2:advanced_machine','X':'#c:ingots/silver'})
shaped('complex_circuit','ic2:complex_circuit',['XYX','CVC','XYX'],{'X':'ic2:double_insulated_gold_cable','Y':'ic2:pulsating_quartz','C':'ic2:magnet','V':'ic2:advanced_circuit'})
shaped('turbine_blade','ic2:turbine_blade',['X X',' X ','X X'],{'X':'#c:ingots/refined_iron'})
for i,locale in enumerate(['en_us','ru_ru']):
    path=root/f'assets/ic2/lang/{locale}.json'
    d=json.loads(path.read_text('utf-8'))
    d.update({k:v[i] for k,v in {
        'block.ic2.rare_earth_extractor':('Rare Earth Extractor','Экстрактор редкоземельных материалов'),
        'block.ic2.centrifugal_rare_earth_extractor':('Centrifugal Rare Earth Extractor','Центробежный экстрактор редкоземельных материалов'),
        'block.ic2.alloy_smelter':('Alloy Smelter','Сплавная печь'),
        'tooltip.ic2.classic.material_progress':('Accumulated material: %s%%','Накоплено материала: %s%%'),
        'tooltip.ic2.classic.speed':('Rotor speed','Скорость ротора')}.items()})
    d['gui.ic2.classic.speed_label']=['Speed','Обороты'][i]
    d['gui.ic2.classic.speed_percent']='%s%%'
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n','utf-8')
manifest_path.write_text(json.dumps(dict(sorted(manifest.items())),ensure_ascii=False,indent=2)+'\n','utf-8')
print('Added original processing textures and material/bronze recipes.')
with zipfile.ZipFile(archive) as z:
    item_model('electrolyzed_water_cell',asset(z,'assets/ic2/textures/item/cells/electrolyzed_water.png'))
manifest_path.write_text(json.dumps(dict(sorted(manifest.items())),ensure_ascii=False,indent=2)+'\n','utf-8')
for i,locale in enumerate(['en_us','ru_ru']):
    path=root/f'assets/ic2/lang/{locale}.json'
    d=json.loads(path.read_text('utf-8'))
    d.update({k:v[i] for k,v in {
        'item.ic2.electrolyzed_water_cell':('Electrolyzed Water Cell','Капсула электролизованной воды'),
        'gui.ic2.electrolyzer.classic':('Electrochemistry','Электрохимия'),
        'gui.ic2.electrolyzer.classic_progress':('Charge: %s / %s EU; recovery buffer: %s EU','Заряд: %s / %s EU; буфер возврата: %s EU')}.items()})
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n','utf-8')

def shaped(name, result, pattern, keys, count=1):
    write(f'data/ic2/recipe/shaped/{name}.json', {'type': 'ic2:shaped', 'pattern': pattern,
          'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v}) for k,v in keys.items()},
          'result': {'item': result, 'count': count}})
with zipfile.ZipFile(archive) as z:
 for name in ['glowtronic_crystal','uesc','pesd']:
    count=1 if name=='pesd' else 5
    for frame in range(count):
        source=f'assets/ic2/textures/item/battery/{name}{"" if name=="pesd" else "_"+str(frame)}.png'
        texture=asset(z,source)
        item_model(name if frame==0 else f'battery/{name}_{frame}',texture)
    if count>1:
        path=root/f'assets/ic2/models/item/{name}.json'
        d=json.loads(path.read_text('utf-8'))
        d['overrides']=[{'predicate':{'ic2:charge':frame/5},'model':f'ic2:item/battery/{name}_{frame}'} for frame in range(1,5)]
        write(path.relative_to(root),d)
 for name in ['esu','isu','pesu']:
    faces={d:asset(z,f'assets/ic2/textures/block/electric/energy_storage/{name}/{d}.png') for d in ['up','down','north','south','east','west']}
    write(f'assets/ic2/models/block/{name}.json',{'parent':'minecraft:block/cube','textures':dict(faces,particle=faces['north'])})
    rotations={'north':{},'south':{'y':180},'east':{'y':90},'west':{'y':270},'up':{'x':270},'down':{'x':90}}
    write(f'assets/ic2/blockstates/{name}.json',{'variants':{f'facing={face}':dict(model=f'ic2:block/{name}',**rot) for face,rot in rotations.items()}})
    write(f'assets/ic2/models/item/{name}.json',{'parent':f'ic2:block/{name}'})
    self_drop(name)
    tag('block','minecraft:mineable/pickaxe',[f'ic2:{name}'])
    tag('block','minecraft:needs_stone_tool',[f'ic2:{name}'])
shaped('glowtronic_crystal','ic2:glowtronic_crystal',['LCL','LDL','LCL'],{'D':'ic2:lapotron_crystal','C':'ic2:complex_circuit','L':'minecraft:glowstone_dust'})
shaped('uesc','ic2:uesc',['XYX','CVC','XYX'],{'X':'ic2:carbon_plate','Y':'ic2:alloy','C':'ic2:iridium','V':'ic2:glowtronic_crystal'})
shaped('pesd','ic2:pesd',['XYX','CVC','XYX'],{'X':'ic2:carbon_plate','Y':'ic2:alloy','C':'ic2:iridium','V':'ic2:plasma_core'})
shaped('esu','ic2:esu',['XVX','YCY','XBX'],{'C':'ic2:machine','Y':'ic2:glowtronic_crystal','X':'ic2:triple_insulated_iron_cable','V':'ic2:advanced_circuit','B':'ic2:stabilized_machine_block'})
shaped('isu','ic2:isu',['LCL','LML','LAL'],{'M':'ic2:esu','A':'ic2:stabilized_machine_block','C':'ic2:iridium','L':'ic2:uesc'})
shaped('pesu','ic2:pesu',['XYX','CCC','XYX'],{'X':'ic2:iridium_stone','Y':'ic2:iridium','C':'ic2:pesd'})
shaped('iridium_stone','ic2:iridium_stone',['XXX','XYX','XXX'],{'X':'ic2:reinforced_stone','Y':'ic2:iridium'},8)
for i,locale in enumerate(['en_us','ru_ru']):
    path=root/f'assets/ic2/lang/{locale}.json'
    d=json.loads(path.read_text('utf-8'))
    d.update({k:v[i] for k,v in {
        'item.ic2.glowtronic_crystal':('Glowtronic Crystal','Глоутронный кристалл'),
        'item.ic2.uesc':('Ultimate Energy Storage Crystal','Ультиматный энергонакопительный кристалл'),
        'item.ic2.pesd':('Plasma Energy Storage Device','Плазменный энергонакопитель'),
        'block.ic2.esu':('ESU','ЭСУ'), 'block.ic2.isu':('ISU','ИСУ'), 'block.ic2.pesu':('PESU','ПЭСУ')}.items()})
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n','utf-8')
manifest_path.write_text(json.dumps(dict(sorted(manifest.items())),ensure_ascii=False,indent=2)+'\n','utf-8')
print('Restored batteries and storage with scaled IC2R capacities.')

colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
prefixes=['','insulated_','double_insulated_','triple_insulated_','quadruple_insulated_']
cables=[]
for metal,maximum in [('bronze',2),('aluminium',4)]:
 for insulation in range(maximum+1): cables.append((prefixes[insulation]+metal+'_cable',metal,insulation))
cables += [('superconductor_cable','super',0),('plasma_cable','plasma',0)]
with zipfile.ZipFile(archive) as z:
 for name,metal,insulation in cables:
    icon=(prefixes[insulation]+metal) if metal not in ['super','plasma'] else metal
    item_model(name,asset(z,f'assets/ic2/textures/item/cable/{icon}.png'))
    colored=insulation>0 or metal=='plasma'
    width=8 if metal in ['super','plasma'] else 4+2*insulation
    multipart=[]
    for color in colors if colored else [None]:
        if metal=='super':
            texture=asset(z,'assets/ic2/textures/block/electric/cable/super/super_side.png')
            cap=asset(z,'assets/ic2/textures/block/electric/cable/super/super_top.png')
        elif metal=='plasma':
            texture=asset(z,f'assets/ic2/textures/block/electric/cable/plasma/{"blank" if color=="black" else color}.png')
            cap=texture
        elif insulation:
            folder=f'{[0,1,2,4,8][insulation]}x_insulated_{metal}'
            texture=asset(z,f'assets/ic2/textures/block/electric/cable/{folder}/{"blank" if color=="black" else color}.png')
            cap=texture
        else:
            texture=asset(z,f'assets/ic2/textures/block/electric/cable/{metal}.png')
            cap=texture
        model=f'cable/classic/{name}'+('_'+color if color else '')
        for part in ['core','side']:
            # Reuse native cable geometry, retaining the original Classic textures byte-for-byte.
            geometry=json.loads((root/f'assets/ic2/models/block/cable/shape/{part}_{width}.json').read_text('utf-8'))
            geometry.pop('credit',None)
            geometry['textures']={'texture':texture,'particle':texture,'cap':cap}
            if metal=='super':
                for element in geometry['elements']:
                    for direction,face in element['faces'].items():
                        if direction=='north' or (part=='core' and direction in ['up','down','south']): face['texture']='#cap'
            write(f'assets/ic2/models/block/{model}_{part}.json',geometry)
        color_condition={'color':color} if color else {}
        core={'apply':{'model':f'ic2:block/{model}_core'}}
        if color_condition: core['when']=color_condition
        multipart.append(core)
        for direction,rot in [('north',{}),('east',{'y':90}),('south',{'y':180}),('west',{'y':270}),('up',{'x':270}),('down',{'x':90})]:
            multipart.append({'when':dict(color_condition,**{direction:'true'}),'apply':dict(model=f'ic2:block/{model}_side',uvlock=True,**rot)})
    write(f'assets/ic2/blockstates/{name}.json',{'multipart':multipart})
    foam=json.loads((root/'assets/ic2/blockstates/copper_foam_cable.json').read_text('utf-8'))
    write(f'assets/ic2/blockstates/{name}_foam.json',foam)
    self_drop(name)
    tag('block','minecraft:mineable/pickaxe',[f'ic2:{name}',f'ic2:{name}_foam'])
 for name,path in {'advanced_alloy_ingot':'ingots/advanced_alloy','polished_gold_ingot':'ingots/polished_gold','uncompressed_superconductor':'misc/uncompressed_super_cable'}.items():
    item_model(name,asset(z,f'assets/ic2/textures/item/{path}.png'))
 for active in [False,True]:
    state='active' if active else 'inactive'
    faces={face:asset(z,f'assets/ic2/textures/block/electric/transformer/iv/{state}_{face}.png') for face in ['up','down','north','south','east','west']}
    write(f'assets/ic2/models/block/iv_transformer_{state}.json',{'parent':'minecraft:block/cube','textures':dict(faces,particle=faces['north'])})
 variants={}
 for active in [False,True]:
    for facing,rot in {'north':{},'south':{'y':180},'east':{'y':90},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items():
        variants[f'active={str(active).lower()},facing={facing}']=dict(model=f'ic2:block/iv_transformer_{"active" if active else "inactive"}',**rot)
 write('assets/ic2/blockstates/iv_transformer.json',{'variants':variants})
 write('assets/ic2/models/item/iv_transformer.json',{'parent':'ic2:block/iv_transformer_inactive'})
 self_drop('iv_transformer')
 tag('block','minecraft:mineable/pickaxe',['ic2:iv_transformer'])
shaped('bronze_cable','ic2:bronze_cable',['XXX'],{'X':'#c:ingots/bronze'},6)
shaped('aluminium_cable','ic2:aluminium_cable',['XXX'],{'X':'#c:ingots/aluminium'},3)
for name,metal,insulation in cables:
 if insulation:
    previous=prefixes[insulation-1]+metal+'_cable'
    write(f'data/ic2/recipe/shapeless/{name}.json',{'type':'ic2:shapeless','ingredients':[{'item':f'ic2:{previous}'},{'item':'ic2:rubber'}],'result':{'item':f'ic2:{name}'}})
shaped('advanced_alloy_ingot','ic2:advanced_alloy_ingot',['III','BBB','AAA'],{'I':'#c:ingots/refined_iron','B':'#c:ingots/bronze','A':'#c:ingots/aluminium'},2)
machine('compressor','advanced_alloy_ingot_to_plate','ic2:advanced_alloy_ingot','ic2:alloy')
shaped('uncompressed_superconductor','ic2:uncompressed_superconductor',[' X ','YCY',' X '],{'X':'ic2:carbon_plate','Y':'ic2:pulsating_quartz','C':'ic2:polished_gold_ingot'})
machine('compressor','superconductor_cable','ic2:uncompressed_superconductor','ic2:superconductor_cable')
shaped('plasma_cable','ic2:plasma_cable',['XXX','YCY','XXX'],{'X':'ic2:carbon_plate','Y':'ic2:refined_iron_ingot','C':'ic2:plasma_core'},4)
shaped('iv_transformer','ic2:iv_transformer',['XYX','CVB','XYX'],{'X':'ic2:iridium_stone','Y':'ic2:plasma_cable','C':'ic2:advanced_circuit','V':'ic2:ev_transformer','B':'ic2:pesd'})
names={
 'advanced_alloy_ingot':('Advanced Alloy Ingot','Слиток улучшенного сплава'),
 'polished_gold_ingot':('Polished Gold Ingot','Полированный золотой слиток'),
 'uncompressed_superconductor':('Uncompressed Superconductor','Несжатый сверхпроводник')}
for i,locale in enumerate(['en_us','ru_ru']):
 path=root/f'assets/ic2/lang/{locale}.json'
 d=json.loads(path.read_text('utf-8'))
 d.update({'item.ic2.'+key:v[i] for key,v in names.items()})
 for name,metal,insulation in cables:
    en=(['','Insulated ','Double-Insulated ','4x Insulated ','8x Insulated '][insulation]+metal.title()+' Cable') if metal not in ['super','plasma'] else ('Superconductor Cable' if metal=='super' else 'Plasma Cable')
    ru=(['','Изолированный ','Дважды изолированный ','Четырежды изолированный ','Восемь раз изолированный '][insulation]+('бронзовый' if metal=='bronze' else 'алюминиевый')+' провод') if metal not in ['super','plasma'] else ('Сверхпроводник' if metal=='super' else 'Плазменный провод')
    d['block.ic2.'+name]=[en,ru[0].upper()+ru[1:]][i]
    d['block.ic2.'+name+'_foam']=[en+' in Construction Foam',ru+' в строительной пене'][i]
 d['block.ic2.iv_transformer']=['Insane Voltage Transformer','Трансформатор сверхвысокого напряжения'][i]
 path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n','utf-8')
manifest_path.write_text(json.dumps(dict(sorted(manifest.items())),ensure_ascii=False,indent=2)+'\n','utf-8')
print('Restored 10 cables and IV transformer with original textures and native geometry.')
