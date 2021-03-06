import axios from 'axios';

export const getApiUrl = () => {
    const url = window.location.origin + '/';
    const localUrl = 'http://localhost:8081/';
    return url.indexOf('localhost') !== -1 ? localUrl : url;
};

//console.log('API', getApiUrl());
export const client = axios.create({
    baseURL: getApiUrl(),
    responseType: 'json'
});

export function get(url) {
    //console.log('AXIOS GET', url);
    return client.get(url, {});
}

export function post(url, data) {
    //console.log('AXIOS POST', url, data);
    return client.post(url, data, {});
}

export function getMoviePredictions(movieId) {
    return get("/public/api/v1/recommend/" + movieId);
}

export function getGenrePredictions(movieDescription) {
    return post("/public/api/v1/genres", JSON.stringify({description: movieDescription}));
}
