import pandas as pd

import numpy as np
import matplotlib.pyplot as plt

# set width of bar
barWidth = 0.2

# set height of bar for memory requirement



# set height of bar for memory access
# HTML_file=[11234.45673,12001.345,388.9458739,1301.251385,457.8987919] #Cloudlab
# Image_File=[9283.56745,8567.3456,2173.101538,4037.372025,2423.733818]#Cloudlab
# Video_file=[7959.456,8023.456,5454.51807,7956.187081,7502.013262]#Cloudlab

HTML_file=[64755.20106,68204.14394,818.2347722,1889.305026,806.0557457] #Chameleon
Image_File=[162423.2999,139357.1265,1071.407471,3517.605434,1249.809929] #Chameleon
Video_file=[31122.29063,51192.71765,2551.239677,6395.780047,4280.099586] #Chameleon

# # HTML_file=[0.56642405,0.434486392,759.37,349.93,881.386649] #intercloud
# # Image_File=[10.23506598,10.43042409,930.90,717.45,931.9280826] #intercloud
# # Video_file=[66.44797281,63.6203995,842.80,841.08,880.6041773] #intercloud

# SDarrayTree2=[6,7,6,12,7,8]
# random1arrayTree1=[11,12,13,14,10,13]
# random1arrayTree2=[5,7,6,10,3,4]
# random2arrayTree1=[11,13,11,6,11,6]
# random2arrayTree2=[11,11,10,12,6,7]
patterns = [ "-",  "x", "o"]
# Set position of bar on X axis
r1 = np.arange(len(HTML_file))
r2 = [x + barWidth for x in r1]
r3 = [x + barWidth for x in r2]
# Make the plot
# plt.bar(r1, HTML_file, color='#7f6d5f', width=barWidth, edgecolor='white', label='DI-Tree1')
# plt.bar(r2, Image_File, color='#557f2d', width=barWidth, edgecolor='white', label='DI-Tree2')
# plt.bar(r3, Video_file, color='#2d7f5e', width=barWidth, edgecolor='white', label='SD-Tree1')
# plt.bar(r4, SDarrayTree2, color='#4d5f6a', width=barWidth, edgecolor='white', label='SD-Tree2')
# plt.bar(r5, random1arrayTree1, color='#5677f5', width=barWidth, edgecolor='white', label='random1-Tree1')
# plt.bar(r6, random1arrayTree2, color='#857d5f', width=barWidth, edgecolor='white', label='random1-Tree2')
# plt.bar(r7, random2arrayTree1, color='#117f5e', width=barWidth, edgecolor='white', label='random2-Tree1')
# plt.bar(r8, random2arrayTree2, color='#37f5ec', width=barWidth, edgecolor='white', label='random2-Tree2')
fig,axes=plt.subplots()

plt.bar(r1, HTML_file, color='white', width=barWidth, edgecolor='black', label='small files',hatch=patterns[0])
plt.bar(r2, Image_File, color='white', width=barWidth, edgecolor='black', label='medium files',hatch=patterns[1])
plt.bar(r3, Video_file, color='white', width=barWidth, edgecolor='black', label='large files',hatch=patterns[2])
# plt.bar(r4, SDarrayTree2, color='white', width=barWidth, edgecolor='black', label='SD-Tree2',hatch=patterns[3])
# plt.bar(r5, random1arrayTree1, color='white', width=barWidth, edgecolor='black', label='random1-Tree1',hatch=patterns[4])
# plt.bar(r6, random1arrayTree2, color='white', width=barWidth, edgecolor='black', label='random1-Tree2',hatch=patterns[5])
# plt.bar(r7, random2arrayTree1, color='white', width=barWidth, edgecolor='black', label='random2-Tree1',hatch=patterns[6])
# plt.bar(r8, random2arrayTree2, color='white', width=barWidth, edgecolor='black', label='random2-Tree2',hatch=patterns[7])


# Add xticks on the middle of the group bars
plt.xlabel('', fontweight='bold',fontsize=16)
plt.yscale('log',base=10)
#following two lines are for memory requirement plot
#plt.ylabel('Bytes per rule (in logscale)', fontweight='bold',fontsize=16)
#plt.yscale('log')

#following  line is for memory access plot
plt.ylabel('Average Energy Consumption(Joules)', fontweight='bold',fontsize=16)
plt.xticks([r + barWidth for r in range(len(HTML_file))], ["curl","wget","Min Energy HLA with DT","Di Tacchio","Max Tput HLA with DT"],rotation = 45)

# Create legend & Show graphic
#plt.legend()
plt.legend(loc='upper right',prop={'size': 16},bbox_to_anchor=(1, 1),
          ncol=1,fancybox=True, shadow=True)

# fig.savefig('figures/cloudLab_Energy.png',dpi=fig.dpi)
fig.savefig('figures/chameleon_Energy.png',dpi=fig.dpi)
# fig.savefig('figures/intercloud_Energy.png',dpi=fig.dpi)
plt.show()
