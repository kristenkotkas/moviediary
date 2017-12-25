import React from 'react';
import './static/css/App.css';
import DraggablePoster from './components/DraggablePoster';
import {moviesData} from './data/data';
import SidePoster from './components/SidePoster';

export default class App extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      screenWidth: 0,
      screenHeight: 0,
      likedMovies: [],
      dislikedMovies: [],
      moviesData: moviesData.slice(0, 100)
    };
    console.log('Total movies count', moviesData.length);
  }

  componentWillMount() {
    this.updateWindowDimensions();
    window.addEventListener('resize', this.updateWindowDimensions.bind(this));
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.updateWindowDimensions.bind(this));
  }

  updateWindowDimensions() {
    this.setState({
      screenWidth: window.innerWidth,
      screenHeight: window.innerHeight
    });
  }

  stopHandler(movieData) {
    if (movieData.state.imageXPosition / this.state.screenWidth <= 0.1) {
      if (this.state.dislikedMovies.length < 6) {
        console.log('You don\'t like ' + movieData.data.movieTitle + '.');
        this.addToDislikes(movieData.data);
      }
    } else if (movieData.state.imageXPosition / this.state.screenWidth >= 0.9) {
      if (this.state.likedMovies.length < 6) {
        console.log('You like ' + movieData.data.movieTitle + '.');
        this.addToLikes(movieData.data);
      }
    }
  }

  addToLikes(movieData) {
    this.setState({
      likedMovies: [...this.state.likedMovies, movieData]
    }, this.removeFromMovies(movieData));
  }

  addToDislikes(movieData) {
    this.setState({
      dislikedMovies: [...this.state.dislikedMovies, movieData]
    }, this.removeFromMovies(movieData));
  }

  removeFromMovies(movieData) {
    this.setState({
      moviesData: this.state.moviesData.filter(movie => movie.movieId !== movieData.movieId)
    });
  }

  removeMovieFromSide(movieData) {
    this.setState({
      likedMovies: this.state.likedMovies.filter(movie => movie.movieId !== movieData.movieId),
      dislikedMovies: this.state.dislikedMovies.filter(movie => movie.movieId !== movieData.movieId),
      moviesData: [...this.state.moviesData, movieData]
    });
  }

  render() {
    return (
        <div style={{margin: '1em 0'}}>
          {this.state.dislikedMovies.map((movie, key) => {
            return <SidePoster
                key={movie.movieTitle}
                movieTitle={movie.movieTitle}
                moviePosterPath={movie.moviePosterPath}
                movieId={movie.movieId}
                xPos={0.01 * this.state.screenWidth}
                yPos={key * 110}
                removeMovie={this.removeMovieFromSide.bind(this)}
            />;
          })}
          {this.state.moviesData.map((movie, key) => {
            return <DraggablePoster
                key={movie.movieTitle}
                stopHandler={this.stopHandler.bind(this)}
                movieTitle={movie.movieTitle}
                moviePosterPath={movie.moviePosterPath}
                screenSize={{
                  screenWidth: this.state.screenWidth,
                  screenHeight: this.state.screenHeight
                }}
                data={movie}
                position={key}
            />;
          })}
          {this.state.likedMovies.map((movie, key) => {
            return <SidePoster
                key={movie.movieTitle}
                movieTitle={movie.movieTitle}
                moviePosterPath={movie.moviePosterPath}
                movieId={movie.movieId}
                xPos={0.91 * this.state.screenWidth}
                yPos={key * 110}
                removeMovie={this.removeMovieFromSide.bind(this)}
            />;
          })}
        </div>
    );
  }
}
