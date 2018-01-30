import React from 'react';
import ListPoster from "./components/ListPoster";
import {moviesData} from './data/data';

export default class CompDemo extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      moviesData: moviesData
    }
  }

  shortenTitle(title) {
    //title = title.split(":");
    //title = title[title.length - 1];
    return title;
  }

  removeMovie(movieId) {
    console.log("Removed", movieId);
    this.setState({
      moviesData: this.state.moviesData.filter(movie => movie.movieId !== movieId)
    });
  }

  render() {
    return (
      <div>
        <h1>Comp demo</h1>
        {this.state.moviesData.map((movie, key) => {
          return (
              <ListPoster
                  movieTitle={this.shortenTitle(movie.movieTitle)}
                  moviePosterPath={movie.moviePosterPath}
                  movieRating={movie.movieId}
                  movieId={movie.movieId}
                  key={key}
                  removeCallback={this.removeMovie.bind(this)}
              />
          )
        })}
      </div>
    );
  }
}
