# https://alyssaq.github.io/2015/20150426-simple-movie-recommender-using-svd/
# data
# https://grouplens.org/datasets/movielens/

import numpy as np
import pandas as pd

np.seterr(divide='ignore', invalid='ignore')

data = pd.io.parsers.read_csv('data/final-new-ratings.csv',
                              names=['user_id', 'movie_id', 'rating', 'time'],
                              engine='python', delimiter=';')
movie_data = pd.io.parsers.read_csv('data/final-new-movies.csv',
                                    names=['movie_id', 'title', 'genre'],
                                    engine='python', delimiter=';')

ratings_mat = np.ndarray(
    shape=(np.max(data.movie_id.values), np.max(data.user_id.values)),
    dtype=np.uint8)
ratings_mat[data.movie_id.values - 1, data.user_id.values - 1] = data.rating.values

normalised_mat = ratings_mat - np.matrix(np.mean(ratings_mat, 1)).T
print(normalised_mat.shape)
cov_mat = np.cov(normalised_mat)
model = np.linalg.eig(cov_mat)
evals, evecs = np.linalg.eig(cov_mat)


def top_cosine_similarity(data, movie_id, top_n=10):
    index = movie_id - 1  # Movie id starts from 1
    movie_row = data[index, :]
    magnitude = np.sqrt(np.einsum('ij, ij -> i', data, data))
    similarity = np.dot(movie_row, data.T) / (magnitude[index] * magnitude)
    sort_indexes = np.argsort(-similarity)
    return (sort_indexes[:top_n], similarity)

#"kasutatav filmi id": tmdb filmi id
lookup = {
    "71": 862,
    "66": 197,
    "67": 278,
    "68": 3049,
    "69": 8587,
    "62": 78,
    "63": 771,
    "3": 114,
    "64": 627,
    "65": 238,
    "70": 872,
    "4": 567,
    "5": 770,
    "6": 62,
    "7": 88,
    "8": 601,
    "9": 85,
    "10": 348,
    "11": 703,
    "12": 694,
    "13": 914,
    "14": 621,
    "15": 578,
    "2": 816,
    "16": 18,
    "17": 597,
    "18": 1725,
    "19": 11252,
    "20": 8741,
    "21": 11167,
    "22": 603,
    "23": 509,
    "1": 2105,
    "24": 550,
    "25": 10784,
    "26": 392,
    "27": 77,
    "28": 808,
    "29": 676,
    "30": 585,
    "31": 120,
    "32": 453,
    "33": 855,
    "34": 425,
    "35": 672,
    "36": 423,
    "37": 12,
    "38": 22,
    "39": 24,
    "40": 11846,
    "41": 38,
    "42": 11036,
    "43": 6947,
    "44": 9806,
    "45": 477433,
    "46": 591,
    "47": 920,
    "48": 350,
    "49": 1858,
    "50": 7326,
    "51": 155,
    "52": 8966,
    "53": 13223,
    "54": 19995,
    "55": 50014,
    "56": 84892,
    "57": 157336,
    "58": 207703,
    "59": 140607,
    "60": 286217,
    "61": 259693,
}


# Helper function to print top N similar movies
def get_similar_movies(movie_data, movie_id, top_indexes):
    print('Recommendations for {0}: \n'.format(
        movie_data[movie_data.movie_id == movie_id].title.values[0]))
    result = []
    for id in top_indexes[0] + 1:
        result.append({
            "tmdb_id": lookup[str(movie_data[movie_data.movie_id == id].movie_id.values[0])],
            "similarity": top_indexes[1][id - 1],
            "title": movie_data[movie_data.movie_id == id].title.values[0]
        })
    return {"result": result}


k = 25
top_n = 70

sliced = evecs[:, :k]  # representative data


def getData(movie_id):
    movie_id = int(movie_id)
    top_indexes = top_cosine_similarity(sliced, movie_id, top_n)
    return get_similar_movies(movie_data, movie_id, top_indexes)


print(getData("54"))
