"""Create one-block workstation overlays and additive village pools from Minecraft 1.21.1.

Usage: python tools/generate_village_workshops.py <minecraft-client.jar>
Requires nbtlib. Vanilla building artwork/templates stay in the base game and are
referenced at runtime; only the original IC2 workstation overlay is written here.
"""
from pathlib import Path
import json, zipfile, nbtlib, io, gzip, copy, sys
r = Path(__file__).resolve().parents[1]
z=zipfile.ZipFile(sys.argv[1])
roles={'electric':('iron_furnace',2),'crop':('cropmatron',2),'brewing':('barrel',2),'demo':('itnt',2),'nuclear':('nuclear_reactor',1),'greg':('industrial_workbench',1)}
manifest=[]
for biome in ['plains','desert','savanna','taiga','snowy']:
 house=f'village/{biome}/houses/{biome}_small_house_1'
 f=nbtlib.File.parse(io.BytesIO(gzip.decompress(z.read(f'data/minecraft/structure/{house}.nbt'))))
 palette=f['palette'];blocks={tuple(map(int,b['pos'])):str(palette[int(b['state'])]['Name']) for b in f['blocks']}
 entry=next(tuple(map(int,b['pos'])) for b in f['blocks'] if b.get('nbt',{}).get('name')=='minecraft:building_entrance')
 sx,sy,sz=map(int,f['size']);air={'minecraft:air','minecraft:cave_air'};candidates=[]
 for pos,name in blocks.items():
  x,y,zz=pos
  if not (1<=x<sx-1 and 1<=zz<sz-1 and y<=3 and name in air):continue
  if blocks.get((x,y+1,zz)) not in air:continue
  floor=blocks.get((x,y-1,zz),'minecraft:air')
  if not any(x in floor for x in ['planks','sandstone','snow_block','cobblestone','acacia_wood','grass_block']):continue
  if sum(blocks.get((x+dx,y,zz+dz)) in air for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)])<1:continue
  candidates.append((sum((a-b)**2 for a,b in zip(pos,entry)),pos))
 assert candidates,biome
 pos=max(candidates)[1]
 pool=json.loads(z.read(f'data/minecraft/worldgen/template_pool/village/{biome}/houses.json'))
 base=next(e['element'] for e in pool['elements'] if e['element'].get('location')=='minecraft:'+house)
 entries=[]
 for role,(block,weight) in roles.items():
  overlay=f'village/{biome}/workstations/{role}'
  root=nbtlib.Compound({'DataVersion':nbtlib.Int(3955),'size':nbtlib.List[nbtlib.Int]([sx,sy,sz]),'palette':nbtlib.List[nbtlib.Compound]([nbtlib.Compound({'Name':nbtlib.String('ic2:'+block)})]),'blocks':nbtlib.List[nbtlib.Compound]([nbtlib.Compound({'pos':nbtlib.List[nbtlib.Int](pos),'state':nbtlib.Int(0)})]),'entities':nbtlib.List[nbtlib.Compound]([])})
  target=r/f'src/main/resources/data/ic2/structure/{overlay}.nbt';target.parent.mkdir(parents=True,exist_ok=True)
  out=io.BytesIO();nbtlib.File(root).write(out);target.write_bytes(gzip.compress(out.getvalue(),mtime=0))
  entries.append({'weight':weight,'element':{'element_type':'minecraft:list_pool_element','elements':[copy.deepcopy(base),{'element_type':'minecraft:single_pool_element','location':'ic2:'+overlay,'processors':{'processors':[]},'projection':'rigid'}],'projection':'rigid'}})
 path=r/f'src/main/resources/data/ic2/worldgen/template_pool/village/{biome}/workshops.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps({'fallback':'minecraft:empty','elements':entries},indent=2)+'\n')
 old=sum(e['weight'] for e in pool['elements']);manifest.append({'biome':biome,'vanilla_pool_weight':old,'base_house':house,'workstation_pos':pos,'new_pool_weight':old+10,'weights':{role:w for role,(_,w) in roles.items()}})
 print(biome,old,'workstation',pos)
p=r/'docs/village-workshop-generation.json';p.write_text(json.dumps({'minecraft':'1.21.1','vanilla_weight_multiplier':1,'variants':manifest},indent=2)+'\n')
