"""Bake disjoint metal/glass pipe shells for all 64 connection states. Uses original textures."""
from collections import defaultdict
from itertools import product
from pathlib import Path
import json

DIRECTIONS = [('down', (0,-1,0)), ('up', (0,1,0)), ('north', (0,0,-1)), ('south', (0,0,1)), ('west', (-1,0,0)), ('east', (1,0,0))]
SIZES = [('tiny',4), ('small',6), ('medium',8), ('large',10)]

def native_uv(lo, hi, direction):
    if direction == 'down': return [lo[0],16-hi[2],hi[0],16-lo[2]]
    if direction == 'up': return [lo[0],lo[2],hi[0],hi[2]]
    if direction == 'north': return [16-hi[0],16-hi[1],16-lo[0],16-lo[1]]
    if direction == 'south': return [lo[0],16-hi[1],hi[0],16-lo[1]]
    if direction == 'west': return [lo[2],16-hi[1],hi[2],16-lo[1]]
    return [16-hi[2],16-hi[1],16-lo[2],16-lo[1]]

def conforming_faces(elements):
    """Split shared edges at every structural vertex, preserving the original texture mapping."""
    cuts=[sorted({element[bound][axis] for element in elements for bound in ('from','to')}) for axis in range(3)]
    result=[]
    for element in elements:
        lo,hi=element['from'],element['to']
        direction,face=next(iter(element['faces'].items()))
        step=dict(DIRECTIONS)[direction]
        axis=next(i for i,n in enumerate(step) if n);u=(axis+1)%3;v=(axis+2)%3
        us=[n for n in cuts[u] if lo[u]<=n<=hi[u]]
        vs=[n for n in cuts[v] if lo[v]<=n<=hi[v]]
        original_uv=native_uv(lo,hi,direction)
        for x0,x1 in zip(us,us[1:]):
            for y0,y1 in zip(vs,vs[1:]):
                start=lo.copy();end=hi.copy()
                start[u]=x0;end[u]=x1;start[v]=y0;end[v]=y1
                piece=json.loads(json.dumps(element));piece['from']=start;piece['to']=end
                if face['texture']=='#metal':
                    uv=native_uv(start,end,direction)
                    piece['faces'][direction]['uv']=[
                        round((value-original_uv[i%2])/(original_uv[i%2+2]-original_uv[i%2])*16,8)
                        for i,value in enumerate(uv)]
                result.append(piece)
    return result

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
            # Include diagonals: a concave elbow corner must remain a full metal pixel.
            border=2 if variant=='heavy' else 1
            offsets=list(product(range(-border,border+1),repeat=2))
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
            # Glazing has one outward surface, avoiding stacked tint layers through
            # the same thin wall. The opaque metal still encloses its full thickness.
            if material=='glass' and full(q): continue
            axis=next(j for j,v in enumerate(step) if v);u=(axis+1)%3;v=(axis+2)%3
            faces[(i,p[axis]+(step[axis]>0),material)].add((p[u],p[v]))
    elements=[];highlights=[]
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
            else:face['neoforge_data']={'color':'4dbed9e6','ambient_occlusion':False}
            element={'from':lo,'to':hi,'faces':{direction:face}}
            if material=='glass':element['shade']=False
            elements.append(element)
            if material=='glass' and w>=2 and h>=2:
                # Keep only the three painted pixels of the vanilla diagonal. A full
                # translucent 3x3 decal can leave a dark rectangular sorting/mipmap halo.
                marks=[(x+.25,y+.25)]
                if w>=7 and h>=5:marks.append((x+w-1.75,y+h-1.75))
                for mx,my in marks:
                    for px,py in ((0,2),(1,1),(2,0)):
                        glint_lo=lo.copy();glint_hi=hi.copy()
                        glint_lo[axis]=glint_hi[axis]=plane+step[axis]*.01
                        glint_lo[u]=mx+px*.5;glint_hi[u]=glint_lo[u]+.5
                        glint_lo[v]=my+py*.5;glint_hi[v]=glint_lo[v]+.5
                        glint={'texture':'#shine','uv':[0,0,1,1],
                               'neoforge_data':{'color':'ffe4f2f9','ambient_occlusion':False}}
                        highlights.append({'from':glint_lo,'to':glint_hi,'shade':False,'faces':{direction:glint}})
    elements=conforming_faces(elements)
    textures={'metal':'ic2:block/experimental/pipe_side','glass':'minecraft:block/white_stained_glass','shine':'minecraft:block/white_stained_glass','particle':'ic2:block/experimental/pipe_side'}
    frame=[e for e in elements if next(iter(e['faces'].values()))['texture']=='#metal']+highlights
    glazing=[e for e in elements if next(iter(e['faces'].values()))['texture']=='#glass']
    return {'parent':'minecraft:block/block','loader':'neoforge:composite','textures':textures,
            'children':{'frame':{'parent':'minecraft:block/block','render_type':'minecraft:solid','textures':textures,'elements':frame},
                        'glazing':{'parent':'minecraft:block/block','render_type':'minecraft:translucent','textures':textures,'elements':glazing}},
            'item_render_order':['frame','glazing']}

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
