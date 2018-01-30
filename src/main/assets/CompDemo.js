import React from 'react';
import ListPoster from "./components/ListPoster";
import {moviesData} from './data/data';

export default class CompDemo extends React.Component {

  constructor(props) {
    super(props);
  }

  shortenTitle(title) {
    //title = title.split(":");
    //title = title[title.length - 1];
    return title;
  }

  render() {
    return (
      <div>
        <h1>Comp demo</h1>
        {moviesData.map((movie, key) => {
          return (
              <ListPoster
                  movieTitle={this.shortenTitle(movie.movieTitle)}
                  moviePosterPath={movie.moviePosterPath}
                  movieRating={movie.movieId}
                  key={key}
              />
          )
        })}
      </div>
    );
  }
}
