# Performance analysis and comparison with CEWE-myphotos' HP Face Clustering Service

## General Observations

### Performance

TODO

### Face Detection

The Face Detection App, which uses the OpenCV Haar Cascade model has natural difficulties to detect faces with following features :
 
    - "cut" or cropped faces
    
    - faces in profile, or with an orientation that does not show the whole face
    
    - faces with dark skin complexion or lighting

The app tends also to detect sometimes faces where there are none (especially where shadows create a semblance of face). 
The number of these false positives increases with high definition pictures.
A path to correcting that could be an elimination of the "faces" that do not have a skin color tone.

### Face Clustering

Both services tend to get different faces with sunglasses mixed up.


## Comparative Tests

The 2 tests have been carried out to compare the clustering accuracy of the app and of the Face Clustering service used by CEWE MyPhotos, built by HP.

We consider the number of faces correctly detected and clustered of each person.

A cluster contains at least 2 faces

**Notation** :

**S** : *Separated* means that the faces of the person have been correctly clustered together, but in not one but several clusters. 
      "S3" means they are scattered among 3 clusters.
      
**M** : *Mixed up* means that the faces of the person have been correctly clustered together, but mixed up with another person's faces in the same cluster.

### Comparative Test 1

The data is composed of 60 pictures containing 114 faces belonging to 13 different persons, as follows :

| **Person Id**   | A  | B  | C  | D | E  | F | G | H | I | J | K | L | M |
|-----------------|----|----|----|---|----|---|---|---|---|---|---|---|---|
| **Nb of Faces** | 22 | 20 | 18 | 5 | 12 | 7 | 4 | 2 | 5 | 5 | 5 | 6 | 3 |

The maximum clustering distance of the app's DBSCAN algorithm was set at **0.5**.

| **Person Id**             |       A      |      B      |      C      |      D     |      E      |      F      |  G  |  H  |     I     |  J  |  K  |  L  |   M  | Other                     |
|---------------------------|:------------:|:-----------:|:-----------:|:----------:|:-----------:|:-----------:|:---:|:---:|:---------:|:---:|:---:|:---:|:----:|---------------------------|
| **App success rate**      |     0.78     |  0.8 **M**  | 0.78 **M**  | 0.5 **M**  | 0.33 **M**  |    0.86     | 0.0 | 0.0 | 0.8 **M** | 0.8 | 1.0 | 1.0 | 0.67 | 1 false positives cluster |
| **MyPhotos success rate** |  0.5 **S4**  | 0.35 **S3** | 0.39 **S3** | 1.0 **S3** | 0.42 **S1** | 0.71 **S1** | 0.0 | 0.0 |    0.0    | 0.6 | 0.4 | 1.0 |  0.0 |                           |

The clustering threshold of the app is highly confident, which explains the many "mixed up" clusters that contain the faces of several persons.
This also allows the app to have globally a best accuracy than MyPhotos, with larger clusters and almost no "separated" clusters.

### Comparative Test 2

The data is composed of 135 pictures containing XXX faces belonging to 9 different persons, as follows :

| **Person Id**   |  A |  B |  C |  D | E  |  F |  G |  H | I |
|-----------------|:--:|:--:|:--:|:--:|----|:--:|:--:|:--:|:-:|
| **Nb of Faces** | 67 | 62 | 59 | 53 | 21 | 10 | 12 | 10 | 7 |

The maximum clustering distance of the app's DBSCAN algorithm was set at several values : 0.43, 0.45 ans 0.48.

| **Person Id**                          |      A      |      B      |      C      |      D      |      E      |      F     |   G  |  H  |      I     | Other              |
|----------------------------------------|:-----------:|:-----------:|:-----------:|:-----------:|:-----------:|:----------:|:----:|:---:|:----------:|--------------------|
| **App success rate** threshold = 0.43  | 0.32 **S3** |     0.24    |  0.3 **S5** | 0.28 **S2** |     0.47    |     0.0    | 0.58 | 0.2 |     0.0    | 2 rubbish clusters |
| **App success rate** threshold = 0.45  | 0.49 **S4** | 0.32 **S2** |  0.41 **M** | 0.34 **S2** |  0.52 **M** |     0.2    | 0.75 | 0.2 |     0.0    |                    |
| **App success rate** threshold = 0.48  |  0.61 **M** |     0.48    |  0.64 **M** |  0.43 **M** |  0.57 **M** |     0.3    | 0.92 | 0.2 | 0.57 **M** |                    |
| **MyPhotos success rate**              | 0.46 **S2** | 0.56 **S2** | 0.24 **S5** | 0.49 **S4** | 0.67 **S3** | 0.9 **S3** | 0.25 | 0.3 |    0.29    |                    |

Reducing the "confidence threshold" decreases drastically the accuracy of the app's clustering.