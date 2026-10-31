"""Render the actual shipped item geometry. These are model previews, NOT Minecraft screenshots."""
from pathlib import Path
import json, math
import numpy as np
from PIL import Image
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
root=Path(__file__).resolve().parents[1]
model=json.loads((root/'src/main/resources/assets/lc_hollowforged/models/item/hollow_core.json').read_text())
texture=Image.open(root/'src/main/resources/assets/lc_hollowforged/textures/item/hollow_core_icon.png').convert('RGBA')
fig=plt.figure(figsize=(12,4),facecolor='#303036',layout='constrained')
for index,(title,context) in enumerate([('Heart relief — front',None),('Heart relief — angled','angled'),('Ground transform — flat','ground')],1):
 ax=fig.add_subplot(1,3,index,projection='3d');ax.set_facecolor('#303036');polygons=[];colors=[]
 for element in model['base']['elements']:
  a=np.array(element['from'])-8;b=np.array(element['to'])-8
  x0,y0,z0=a;x1,y1,z1=b
  vertices=np.array([[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]])
  if context=='ground':
   # Minecraft ground transform: 90 degrees about X; remap Minecraft Y-up to plot Z-up below.
   vertices=vertices@np.array([[1,0,0],[0,0,1],[0,-1,0]])
  # Matplotlib uses Z-up; Minecraft uses Y-up.
  vertices=vertices[:,[0,2,1]]
  uv=element['faces']['south']['uv'];color=np.array(texture.getpixel((int(uv[0]),int(uv[1]))))/255
  for ids,light in [([0,1,2,3],.8),([4,7,6,5],1),([0,4,5,1],.58),([3,2,6,7],1),([0,3,7,4],.65),([1,5,6,2],.85)]:
   polygons.append(vertices[ids]);colors.append(tuple(np.clip(color[:3]*light,0,1))+(1,))
 ax.add_collection3d(Poly3DCollection(polygons,facecolors=colors,edgecolors='none',zsort='average'))
 ax.set(xlim=(-8,8),ylim=(-8,8),zlim=(-8,8));ax.set_box_aspect((1,1,1));ax.set_proj_type('ortho');ax.set_axis_off()
 if context is None:ax.view_init(elev=0,azim=-90)
 elif context=='angled':ax.view_init(elev=20,azim=-60)
 else:ax.view_init(elev=32,azim=55)
 ax.set_title(title,color='white',fontsize=12)
fig.suptitle('Hollow Core — previews rendered from the shipped model JSON (not in-game screenshots)',color='white',fontsize=11)
out=root/'build/core-preview';out.mkdir(parents=True,exist_ok=True);fig.savefig(out/'Hollow_Core_Model_Views.png',dpi=150,facecolor=fig.get_facecolor());plt.close(fig)
