"""Import original Experimental artwork/recipes and the separately approved Classic villager skins.

Usage: python tools/restore_experimental_assets.py experimental.jar classic.jar
Requires Pillow, or the task-local build/asset-python installation. Original pixel
art is copied unchanged or enlarged 16-to-32 with nearest-neighbor sampling.
"""
from pathlib import Path
import hashlib
import io
import json
import sys
import zipfile

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / 'build/asset-python'))
from PIL import Image

RES = REPO / 'src/main/resources'
manifest = []


def write(path, data):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, dict): data = (json.dumps(data, ensure_ascii=False, indent=2) + '\n').encode('utf8')
    target.write_bytes(data)


def image(archive, source, target, enlarge=False):
    original = archive.read(source)
    img = Image.open(io.BytesIO(original))
    dimensions = list(img.size)
    data = original
    if (enlarge or '/textures/blocks/' in source) and img.size == (16, 16):
        img = img.resize((32, 32), Image.Resampling.NEAREST)
        out = io.BytesIO(); img.save(out, format='PNG'); data = out.getvalue()
    write(target, data)
    if source + '.mcmeta' in archive.namelist(): write(target + '.mcmeta', archive.read(source + '.mcmeta'))
    manifest.append({'source': source, 'target': target, 'source_size': dimensions,
                     'size': list(img.size), 'source_sha256': hashlib.sha256(original).hexdigest(),
                     'sha256': hashlib.sha256(data).hexdigest()})


def icon(archive, name, source):
    image(archive, source, f'assets/ic2/textures/item/experimental/{name}.png', True)
    write(f'assets/ic2/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'ic2:item/experimental/{name}'}})


def shaped(name, pattern, keys, result, count=1):
    write(f'data/ic2/recipe/shaped/experimental/{name}.json', {'type': 'ic2:shaped', 'pattern': pattern,
        'key': {k: {'tag': v[1:]} if v.startswith('#') else {'item': v} for k, v in keys.items()},
        'result': {'item': result, 'count': count}})


def shapeless(name, ingredients, result, count=1):
    write(f'data/ic2/recipe/shapeless/experimental/{name}.json', {'type': 'ic2:shapeless',
        'ingredients': [{'item': value} for value in ingredients], 'result': {'item': result, 'count': count}})


def cube(textures, start=(0, 0, 0), end=(16, 16, 16), tint=False):
    faces = {direction: {'texture': '#' + direction, **({'tintindex': 0} if tint else {})} for direction in ['north', 'south', 'east', 'west', 'up', 'down']}
    return {'textures': textures, 'elements': [{'from': list(start), 'to': list(end), 'faces': faces}]}


experimental = Path(sys.argv[1]); classic = Path(sys.argv[2])
with zipfile.ZipFile(experimental) as exp, zipfile.ZipFile(classic) as cls:
    image(exp, 'assets/ic2/textures/blocks/machine/misc/item_buffer_2.png',
          'assets/ic2/textures/block/machine/misc/item_buffer_2.png')
    write('assets/ic2/guidef/item_buffer_2.xml', exp.read('assets/ic2/guidef/item_buffer_2.xml')
          .replace(b'<text y="6" align="center">',
                   b'<text x="-4" y="6" align="center" maxwidth="128">'))
    shaped('item_buffer_2', ['ICI', 'IMI', 'ICI'],
           {'I':'ic2:iron_casing', 'C':'#c:chests/wooden', 'M':'ic2:machine'}, 'ic2:item_buffer_2')
    icons = {'solar_helmet': 'armor/solar_helmet', 'static_boots': 'armor/static_boots',
             'containment_box': 'tool/containment_box', 'iodine_tablet': 'iodine_tablet',
             'mfsu_upgrade_kit': 'crafting/mfsu_upgrade_kit', 'fluid_cell': 'cell/fluid_cell',
             'barrel': 'brewing/barrel', 'dynamite': 'dynamite', 'dynamite_sticky': 'dynamite_sticky',
             'remote': 'tool/remote', 'pump_lv': 'cover/pump_foreground', 'pump_mv': 'cover/pump_foreground'}
    for name, path in icons.items(): icon(exp, name, f'assets/ic2/textures/items/{path}.png')
    sound = next(name for name in exp.namelist() if name.lower().endswith('dynamiteomote.ogg'))
    write('assets/ic2/sounds/tools/dynamiteomote.ogg', exp.read(sound))
    sound_path = RES/'assets/ic2/sounds.json'; sounds=json.loads(sound_path.read_text(encoding='utf8'))
    sounds['item.dynamite.remote']={'subtitle':'subtitle.ic2.item.dynamite.remote','sounds':['ic2:tools/dynamiteomote']}
    write('assets/ic2/sounds.json',sounds)
    for name in ['pump_background', 'pump_foreground']:
        image(exp, f'assets/ic2/textures/items/cover/{name}.png', f'assets/ic2/textures/item/experimental/{name}.png', True)
    for tier in ['lv', 'mv']:
        write(f'assets/ic2/models/item/pump_{tier}.json', {'parent':'minecraft:item/generated','textures':{'layer0':'ic2:item/experimental/pump_background','layer1':'ic2:item/experimental/pump_foreground'}})
    image(exp, 'assets/ic2/textures/items/cell/fluid_cell_window.png','assets/ic2/textures/item/experimental/fluid_cell_window.png',True)
    write('assets/ic2/models/item/fluid_cell.json', {'parent':'minecraft:item/generated','textures':{'layer0':'ic2:item/experimental/fluid_cell'},'overrides':[{'predicate':{'ic2:filled':1},'model':'ic2:item/experimental/fluid_cell_filled'}]})
    write('assets/ic2/models/item/experimental/fluid_cell_filled.json', {'parent':'minecraft:item/generated','textures':{'layer0':'ic2:item/experimental/fluid_cell','layer1':'ic2:item/experimental/fluid_cell_window'}})
    for name, source in [('experimental_solar', 'solar'), ('experimental_static', 'rubber')]:
        image(exp, f'assets/ic2/textures/armor/{source}_1.png', f'assets/ic2/textures/models/armor/{name}_layer_1.png')
    image(exp, 'assets/ic2/textures/gui/guicontainmentbox.png', 'assets/ic2/textures/gui/experimental/containment_box.png')
    drinks = ['beer_brew', 'beer_youngster', 'beer_beer', 'beer_ale', 'beer_dragon_blood', 'beer_black_stuff', 'rum']
    for name in drinks: icon(exp, 'booze_' + name, f'assets/ic2/textures/items/brewing/booze_mug_{name}.png')
    write('assets/ic2/models/item/booze_mug.json', {'parent': 'ic2:item/booze_beer_brew', 'overrides': [
        {'predicate': {'custom_model_data': i + 1}, 'model': 'ic2:item/booze_' + name} for i, name in enumerate(drinks)]})
    for part in ['bottomtop', 'sides', 'tap']:
        image(exp, f'assets/ic2/textures/blocks/barrel_{part}.png', f'assets/ic2/textures/block/experimental/barrel_{part}.png')
    for active in [False, True]:
        textures = {d: 'ic2:block/experimental/barrel_' + ('bottomtop' if d in ['up', 'down'] else 'tap' if d == 'north' and active else 'sides') for d in ['north','south','east','west','up','down']}
        textures['particle'] = textures['south']
        write('assets/ic2/models/block/experimental/barrel_' + str(active).lower() + '.json', cube(textures))
    variants = {}
    for facing, angle in [('north',0),('east',90),('south',180),('west',270)]:
        for active in [False,True]: variants[f'facing={facing},active={str(active).lower()}'] = {'model': 'ic2:block/experimental/barrel_' + str(active).lower(), 'y': angle}
    write('assets/ic2/blockstates/barrel.json', {'variants': variants})
    for size in ['tiny','small','medium','large']:
        image(exp, f'assets/ic2/textures/blocks/transport/pipe_{size}.png', f'assets/ic2/textures/block/experimental/pipe_{size}.png')
    image(exp, 'assets/ic2/textures/blocks/transport/pipe_side.png', 'assets/ic2/textures/block/experimental/pipe_side.png')
    for material in ['bronze','steel']:
        for size, width in [('tiny',4),('small',6),('medium',8),('large',10)]:
            name=f'{material}_pipe_{size}'; a=(16-width)/2; b=16-a
            texture={d:'ic2:block/experimental/pipe_' + size for d in ['north','south','east','west','up','down']};texture['particle']=texture['north']
            parent='ic2:block/experimental/' + name
            write(f'assets/ic2/models/block/experimental/{name}.json', cube(texture, (a,a,a),(b,b,b), True))
            parts=[{'apply': {'model': parent}}]
            for direction in ['north','south','east','west','up','down']:
                lo=[a,a,a];hi=[b,b,b]; axis=0 if direction in ['east','west'] else 1 if direction in ['up','down'] else 2
                if direction in ['north','west','down']:lo[axis]=0
                else:hi[axis]=16
                tex={d:'ic2:block/experimental/pipe_side' for d in texture};tex[direction]=texture[direction]
                write(f'assets/ic2/models/block/experimental/{name}_{direction}.json',cube(tex,lo,hi,True))
                parts.append({'when':{direction:'true'},'apply':{'model':parent+'_'+direction}})
            write(f'assets/ic2/blockstates/{name}.json', {'multipart':parts})
            write(f'assets/ic2/models/item/{name}.json', {'parent':parent})
            write(f'data/ic2/loot_table/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ic2:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for linked in [False,True]:
        source='dynamite_remote' if linked else 'dynamite'
        image(exp, f'assets/ic2/textures/blocks/explosive/{source}.png',f'assets/ic2/textures/block/experimental/{source}.png')
        textures={d:'ic2:block/experimental/'+source for d in ['north','south','east','west','up','down']};textures['particle']=textures['north']
        write(f'assets/ic2/models/block/experimental/{source}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':'ic2:block/experimental/'+source,'particle':'ic2:block/experimental/'+source}})
    variants={}
    for side,x,y in [('up',0,0),('down',180,0),('north',270,0),('south',90,0),('east',90,90),('west',90,270)]:
        for linked in [False,True]:variants[f'facing={side},linked={str(linked).lower()}']={'model':'ic2:block/experimental/'+('dynamite_remote' if linked else 'dynamite'),'x':x,'y':y}
    write('assets/ic2/blockstates/dynamite_placed.json',{'variants':variants})
    for kind in ['villager','zombie_villager']:
        for role in ['electric','nuclear','crop','demo','brewing','greg']:
            path=f'assets/ic2/textures/entity/{kind}/profession/{role}.png';image(cls,path,path)

shaped('solar_helmet',['III','ISI','CCC'], {'I':'minecraft:iron_ingot','S':'ic2:solar_generator','C':'ic2:insulated_copper_cable'},'ic2:solar_helmet')
shaped('solar_helmet_from_armor',[' H ',' S ','CCC'], {'H':'minecraft:iron_helmet','S':'ic2:solar_generator','C':'ic2:insulated_copper_cable'},'ic2:solar_helmet')
shaped('static_boots',['I I','ISI','CCC'], {'I':'minecraft:iron_ingot','S':'#minecraft:wool','C':'ic2:insulated_copper_cable'},'ic2:static_boots')
shaped('static_boots_from_armor',[' H ',' S ','CCC'], {'H':'minecraft:iron_boots','S':'#minecraft:wool','C':'ic2:insulated_copper_cable'},'ic2:static_boots')
shaped('barrel',['R','B','R'], {'R':'ic2:rubber','B':'minecraft:barrel'},'ic2:barrel')
shaped('containment_box',['III','ICI','III'], {'I':'ic2:lead_casing','C':'minecraft:chest'},'ic2:containment_box')
shapeless('iodine_tablet',['ic2:iodine','ic2:tin_plate'],'ic2:iodine_tablet')
shaped('mfsu_upgrade_kit',['LCL','LML','LAL'], {'L':'ic2:lapotron_crystal','C':'ic2:advanced_circuit','M':'ic2:wrench','A':'ic2:advanced_machine'},'ic2:mfsu_upgrade_kit')
shaped('fluid_cell',[' C ','CGC',' C '],{'C':'ic2:tin_casing','G':'minecraft:glass_pane'},'ic2:fluid_cell')
shapeless('dynamite',['ic2:itnt','minecraft:string'],'ic2:dynamite',8)
shaped('dynamite_sticky',['DDD','DRD','DDD'],{'D':'ic2:dynamite','R':'ic2:resin'},'ic2:dynamite_sticky',8)
shaped('remote',[' c ','GCG','TTT'],{'c':'ic2:insulated_copper_cable','G':'minecraft:glowstone_dust','C':'ic2:circuit','T':'minecraft:tnt'},'ic2:remote')
shaped('remote_from_transmitter',[' C ','TLT',' F '],{'C':'ic2:insulated_copper_cable','T':'ic2:tin_casing','L':'minecraft:lapis_lazuli','F':'ic2:frequency_transmitter'},'ic2:remote')
for material in ['bronze','steel']:
    for size, pattern, ingredient, count in [('tiny',['CCC','   ','CCC'],'casing',6),('small',['C C','C C','C C'],'casing',3),('medium',['CCC','   ','CCC'],'plate',2),('large',['C C','C C','C C'],'plate',1)]:
        name=f'{material}_pipe_{size}';shaped(name,pattern,{'C':f'ic2:{material}_{ingredient}'},'ic2:'+name,count)
for tier, material, wire in [('lv','bronze','copper'),('mv','steel','gold')]:
    shapeless('pump_'+tier,[f'ic2:{material}_pipe_medium','minecraft:piston',f'ic2:insulated_{wire}_cable'],'ic2:pump_'+tier)
write('data/minecraft/tags/point_of_interest_type/acquirable_job_site.json', {'replace':False,'values':['ic2:'+role for role in ['electric','nuclear','crop','demo','brewing','greg']]})

names={'solar_helmet':('Solar Helmet','Солнечный шлем'),'static_boots':('Static Boots','Статические ботинки'),
 'containment_box':('Containment Box','Защитный контейнер'),'iodine_tablet':('Iodine Tablet','Йодная таблетка'),
 'mfsu_upgrade_kit':('MFSU Upgrade Kit','Комплект улучшения МФСУ'),'fluid_cell':('Universal Fluid Cell','Универсальная жидкостная капсула'),
 'barrel':('Booze Barrel','Бочка для брожения'),'booze_mug':('Booze Mug','Кружка с напитком'),
 'dynamite':('Dynamite','Динамит'),'dynamite_sticky':('Sticky Dynamite','Липкий динамит'),'remote':('Dynamite-O-Mote','Дистанционный подрыватель'),
 'pump_lv':('Pump Attachment (LV)','Насосная насадка НН'),'pump_mv':('Pump Attachment (MV)','Насосная насадка СН')}
roles={'electric':('Electrical Engineer','Электротехник'),'nuclear':('Nuclear Engineer','Ядерщик'),'crop':('Crop Farmer','Агроном'),'demo':('Demolition Man','Подрывник'),'brewing':('Brewing Master','Пивовар'),'greg':('Greg','Грег')}
for index, language in enumerate(['en_us','ru_ru']):
    p=RES/f'assets/ic2/lang/{language}.json';locale=json.loads(p.read_text(encoding='utf8'))
    for name, values in names.items():locale['item.ic2.'+name]=values[index]
    for role, values in roles.items():locale['entity.minecraft.villager.ic2.'+role]=values[index]
    locale['block.ic2.barrel']=names['barrel'][index];locale['container.ic2.containment_box']=names['containment_box'][index]
    locale['entity.ic2.dynamite']=names['dynamite'][index]
    for material in ['bronze','steel']:
        for size in ['tiny','small','medium','large']:
            en=f'{size.title()} {material.title()} Fluid Pipe'
            ru={'tiny':'Крошечная','small':'Малая','medium':'Средняя','large':'Большая'}[size]+' '+('бронзовая' if material=='bronze' else 'стальная')+' жидкостная труба'
            locale[f'block.ic2.{material}_pipe_{size}']=(en,ru)[index]
    entries={'subtitle.ic2.item.dynamite.remote':('Remote detonator activates','Срабатывает дистанционный подрыватель'),
      'message.ic2.experimental.storage_upgraded':('Storage upgraded to MFSU','Хранилище улучшено до МФСУ'),
      'message.ic2.experimental.remote_wrong':('This dynamite is linked to a different remote','Этот динамит привязан к другому подрывателю'),
      'message.ic2.experimental.remote_linked':('Dynamite linked','Динамит привязан'),
      'message.ic2.experimental.remote_unlinked':('Dynamite unlinked','Привязка динамита удалена'),
      'tooltip.ic2.experimental.remote_links':('Linked dynamite: %s','Привязано динамита: %s'),
      'item.ic2.experimental.rum':('Rum','Ром'),'item.ic2.experimental.beer.name':('%s%s%s','%s%s%s')}
    for key,values in entries.items():locale[key]=values[index]
    lists={'solids':(['Watery ','Clear ','Lite ','','Strong ','Thick ','Stodge '],['Водянистое ','Прозрачное ','Лёгкое ','','Крепкое ','Густое ','Вязкое ']),
           'hops':(['Soup ','Alcfree ','White ','','Dark ','Full ','Black '],['суповое ','безалкогольное ','белое ','','тёмное ','насыщенное ','чёрное ']),
           'quality':(['Brew','Youngster','Beer','Ale','Dragonblood','Black Stuff'],['сусло','молодое пиво','пиво','эль','драконья кровь','Чёрная жижа'])}
    for kind, values in lists.items():
        for n, value in enumerate(values[index]):
            key=f'item.ic2.experimental.beer.{kind}.{n}'
            if value: locale[key]=value
            else: locale.pop(key,None)
    p.write_text(json.dumps(locale,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
receipt={'experimental_jar_sha256':hashlib.sha256(experimental.read_bytes()).hexdigest(),
         'classic_jar_sha256':hashlib.sha256(classic.read_bytes()).hexdigest(),
         'classic_scope':'Only original villager and zombie-villager profession textures',
         'scaling':'Only 16x16 item/block textures enlarged to 32x32 with nearest-neighbor sampling', 'images':manifest}
(REPO/'docs/experimental-assets-21.1.70.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
print(f'Imported {len(manifest)} original images and Experimental recipes; preserved villager skins unchanged.')
