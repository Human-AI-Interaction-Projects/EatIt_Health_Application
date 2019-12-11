import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import random
import sys
import scipy as sp
import os
from PIL import Image as im
count=0
def create_img_data():
    images = np.ndarray(100)
    global count
    imagemean = np.zeros((128, 128, 3))
    for filename in os.listdir("final"):
        c1 = 0
        for foodname in os.listdir(filename):
            img = im.open(os.path.join("final", filename, foodname))
            img.thumbnail(128, 128)
            if img is not None:
                np.append(images, np.array(img, dtype="uint8"))
                imagemean += img
                count += 1
            c1 += 1
            if c1 == 30:
                break
    return images, imagmean

images,imagemean=create_img_data()
def cov(images,imagemean):
	global count
	images=np.reshape(images,(count,256*256*3))
	imagemean=np.reshape(imagemean,(1,256*256*3));
	imagemean=np.multiply(imagemean,1/count)
	#cov=np.subtract(images.transpose()-imagemean).transpose()
	#images,imagemean=np.broadcast_arrays(images,imagemean)
	images=np.subtract(images,imagemean)
	return images
covariance=cov(images,imagemean)
