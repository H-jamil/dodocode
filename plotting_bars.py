import pandas as pd

import numpy as np
import matplotlib.pyplot as plt

# set width of bar
barWidth = 0.2

# set height of bar for memory requirement



# set height of bar for memory access
HTML_file=[0.56642405,0.434486392,759.37,349.93,881.386649]
Image_File=[10.23506598,10.43042409,930.90,717.45,931.9280826]
Video_file=[66.44797281,63.6203995,842.80,841.08,880.6041773]
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

#following two lines are for memory requirement plot
#plt.ylabel('Bytes per rule (in logscale)', fontweight='bold',fontsize=16)
#plt.yscale('log')

#following  line is for memory access plot
plt.ylabel('Average Throughput(Mbps)', fontweight='bold',fontsize=16)
plt.xticks([r + barWidth for r in range(len(HTML_file))], ["curl","wget","Min Energy HLA with DT","Di Tacchio","Max Tput HLA with DT"],rotation = 45)

# Create legend & Show graphic
#plt.legend()
plt.legend(loc='upper left',prop={'size': 16},bbox_to_anchor=(0.0, 1),
          ncol=1,fancybox=True, shadow=True)

# fig.savefig('cloudLab_throughput.png',dpi=fig.dpi)
plt.show()
