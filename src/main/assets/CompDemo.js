import React from 'react';
import ListPoster from "./components/ListPoster";
import {moviesData} from './data/posterDemoData';

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

  openMovie(movieId) {
    console.log("Opened", movieId);
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
                    movieYear={movie.movieYear}
                    movieId={movie.movieId}
                    movieRating={movie.movieRating}
                    movieSeen={movie.movieSeen}
                    key={key}
                    removeCallback={this.removeMovie.bind(this)}
                    openCallback={this.openMovie.bind(this)}
                />
            )
          })}
        </div>
    );
  }
}
