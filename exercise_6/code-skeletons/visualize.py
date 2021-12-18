from numpy import complex, array
import numpy as np
import matplotlib.pyplot as plt
import colorsys
import csv
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-f", "--file", dest="target_path",
                  help="data file path", default="mandel_data.csv",  metavar="FILE")

parser.add_option("-N", dest="WIDTH",
                  help="image width", default=1024,  metavar="N")


(options, args) = parser.parse_args()



filename = options.target_path

image_width = int(options.WIDTH)




def i_to_rgb(i):
  color = 255 * array(colorsys.hsv_to_rgb(i/255.0, 1.0, 0.5))
  return tuple(color.astype(int))
      

pixels = [[0 for x in range(image_width)] for y in range(image_width)] 



with open(filename, 'r') as f:
        mycsv = csv.reader(f)
        mycsv = list(mycsv)
        for row in mycsv:
            x = int(row[1]) # x is the column --> the width
            y = int(row[0]) # y is the row --> the height
            value = int(row[2])
            pixels[y][x] = value#i_to_rgb(value)




plt.imshow(pixels, extent=[-2,2,-2,2])

plt.show()

