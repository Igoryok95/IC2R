"""Bake disjoint metal/glass pipe shells for all 64 connection states. Uses original textures."""
from collections import defaultdict
from itertools import product
from pathlib import Path
import json

DIRECTIONS = [('down', (0,-1,0)), ('up', (0,1,0)), ('north', (0,0,-1)), ('south', (0,0,1)), ('west', (-1,0,0)), ('east', (1,0,0))]
SIZES = [('tiny',4), ('small',6), ('medium',8), ('large',10)]

def write(root, name, data):
    path=root/name;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf-8')

def shell(width, mask, variant="light"):
    a=(16-width)//2;b=16-a
    def full(p):
        if all(a<=c<b for c in p): return True
        for i,(_,step) in enumerate(DIRECTIONS):
            axis=next(j for j,v in enumerate(step) if v)
            if mask & (1<<i) and all(a<=p[j]<b for j in range(3) if j!=axis) and (p[axis]<a if step[axis]<0 else p[axis]>=b): return True
        return False
    outer={p for p in product(range(16),repeat=3) if full(p)}
    walls={p for p in outer if not all(full(tuple(p[j]+offset[j] for j in range(3))) for offset in product((-1,0,1),repeat=3))}
    surfaces=defaultdict(set)
    for p in outer:
        for i,(_,step) in enumerate(DIRECTIONS):
            q=tuple(p[j]+step[j] for j in range(3))
            if full(q): continue
            axis=next(j for j,v in enumerate(step) if v);u=(axis+1)%3;v=(axis+2)%3
            plane=p[axis]+(step[axis]>0)
            surfaces[(i,plane)].add((p[u],p[v]))
    glass=set()
    for (i,plane),cells in surfaces.items():
        _,step=DIRECTIONS[i];axis=next(j for j,v in enumerate(step) if v);u=(axis+1)%3;v=(axis+2)%3
        for x,y in cells:
            offsets = list(product(range(-2,3),repeat=2)) if variant=='heavy' else [(-1,0),(1,0),(0,-1),(0,1)]
            if all((x+dx,y+dy) in cells for dx,dy in offsets):
                p=[0,0,0];p[axis]=plane-(step[axis]>0);p[u]=x;p[v]=y
                collar = variant=='collars' and any(p[j] in (a,b-1) for j in range(3) if any(mask & (1<<n) and step2[j] for n,(_,step2) in enumerate(DIRECTIONS)))
                if not collar:glass.add(tuple(p))
    faces=defaultdict(set)
    for p in walls:
        material='glass' if p in glass else 'metal'
        for i,(_,step) in enumerate(DIRECTIONS):
            q=tuple(p[j]+step[j] for j in range(3))
            if q in walls and not (material=='metal' and q in glass): continue
            axis=next(j for j,v in enumerate(step) if v);u=(axis+1)%3;v=(axis+2)%3
            faces[(i,p[axis]+(step[axis]>0),material)].add((p[u],p[v]))
    elements=[]
    for (i,plane,material),cells in sorted(faces.items()):
        direction,step=DIRECTIONS[i];axis=next(j for j,v in enumerate(step) if v);u=(axis+1)%3;v=(axis+2)%3
        while cells:
            x,y=min(cells);w=1
            while (x+w,y) in cells:w+=1
            h=1
            while all((xx,y+h) in cells for xx in range(x,x+w)):h+=1
            cells.difference_update((xx,yy) for xx in range(x,x+w) for yy in range(y,y+h))
            lo=[0,0,0];hi=[0,0,0];lo[axis]=hi[axis]=plane;lo[u]=x;hi[u]=x+w;lo[v]=y;hi[v]=y+h
            face={'texture':'#'+material}
            # Glass uses Minecraft's position-derived UVs. Adjacent patches sample one
            # continuous sheet at native scale instead of repeating a full framed tile.
            if material=='metal':face.update(uv=[0,0,16,16],tintindex=0)
            elements.append({'from':lo,'to':hi,'faces':{direction:face}})
    return {'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'metal':'ic2:block/experimental/pipe_side','glass':'minecraft:block/glass','particle':'ic2:block/experimental/pipe_side'},'elements':elements}

def generate(root):
    for size,width in SIZES:
        for mask in range(64):write(root,f'assets/ic2/models/block/experimental/glass_pipe/{size}_{mask}.json',shell(width,mask))
        for material in ('bronze','steel'):
            name=f'{material}_pipe_{size}';variants={}
            for mask in range(64):
                key=','.join(direction+'='+str(bool(mask&(1<<i))).lower() for i,(direction,_) in enumerate(DIRECTIONS))
                variants[key]={'model':f'ic2:block/experimental/glass_pipe/{size}_{mask}'}
            write(root,f'assets/ic2/blockstates/{name}.json',{'variants':variants})
            write(root,f'assets/ic2/models/block/experimental/{name}.json',{'parent':f'ic2:block/experimental/glass_pipe/{size}_0'})

if __name__=='__main__':generate(Path(__file__).resolve().parents[1]/'src/main/resources')
