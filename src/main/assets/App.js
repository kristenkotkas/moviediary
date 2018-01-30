import React from 'react';
import './static/css/App.css';
import DraggablePoster from './components/DraggablePoster';
import {moviesData} from './data/data';
import SidePoster from './components/SidePoster';
import {getMoviePredictions} from "./utils/AxiosClient";

export default class App extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      screenWidth: 0,
      screenHeight: 0,
      likedMovies: [],
      dislikedMovies: [],
      moviesData: moviesData.slice(0, 100),
      similarityArray: []
    };
    console.log('Total movies count', moviesData);
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
      if (this.state.dislikedMovies.length < 8) {
        console.log('You don\'t like ' + movieData.data.movieTitle + '.');
        this.addToDislikes(movieData.data);
      }
    } else if (movieData.state.imageXPosition / this.state.screenWidth >= 0.9) {
      if (this.state.likedMovies.length < 8) {
        console.log('You like ' + movieData.data.movieTitle + '.');
        this.addToLikes(movieData.data);
      }
    }
  }

  addToLikes(movieData) {
    this.setState({
      likedMovies: [...this.state.likedMovies, movieData]
    }, this.removeFromMovies(movieData));

    getMoviePredictions(movieData.movieId).then(res => {
      console.log("Liked movie response: ", res.data.result);
      this.updatePosterPositions(res.data.result, 1);
    });
  }

  addToDislikes(movieData) {
    this.setState({
      dislikedMovies: [...this.state.dislikedMovies, movieData]
    }, this.removeFromMovies(movieData));

    getMoviePredictions(movieData.movieId).then(res => {
      console.log("Not liked movie response: ", res.data.result);
      this.updatePosterPositions(res.data.result, -1);
    });
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

  getMappedSimilarity(x, a, b, c, d) {
    return ((x - a) / (b - a) * (d - c)) + c
  }

  updatePosterPositions(similarityArray, type) {
    if (this.state.similarityArray.length > 0) {
      console.log(95);
      const newSimilarityArray = similarityArray.map(similarity => {
        let similar = this.state.similarityArray.filter(arrayMovie => arrayMovie['tmdb_id'] === similarity['tmdb_id']);
        /*console.log(similar[0]['similarity']);
        console.log(similarity['similarity']);
        console.log("-----");*/
        console.log("simData", similar[0]['similarity'], type, similarity['similarity']);
        return Object.assign({}, similarity, {
          similarity: similar[0]['similarity'] + (type * similarity['similarity'])
        });
      });
      this.setState({
        similarityArray: newSimilarityArray
      }, this.getNewMovieData);
    } else {
      this.setState({
        similarityArray: similarityArray
      }, this.getNewMovieData);
    }
  }

  getNewMovieData() {
    const count = this.state.likedMovies.length + this.state.dislikedMovies.length;
    console.log("count", count);
    const similarityArray = this.state.similarityArray;
    console.log("similarityArray", similarityArray);
    const similarities = similarityArray.map(elem => elem['similarity'] / count).sort(function (a, b) {
      return a - b
    });
    console.log("similarities", similarities);
    const min = similarities[0];
    const max = similarities[similarities.length - 1];
    console.log("initial range", min, max);

    const data = this.state.moviesData.map(movieData => {
      let similar = similarityArray.filter(simMovie => simMovie['tmdb_id'] === movieData.movieId)[0];
      return Object.assign({}, movieData, {
        xPos: this.getMappedSimilarity(similar['similarity'] / count, min, max, 0.15, 0.85)
      })
    });
    this.setState({
      moviesData: data
    });

    console.log("new movieData", this.state.moviesData);
  }

  reset() {
    this.setState({
      likedMovies: [],
      dislikedMovies: [],
      moviesData: moviesData.slice(0, 100),
      similarityArray: []
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
          <span
              className={'resetButton'}
              onClick={this.reset.bind(this)}
          >Reset</span>
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
                depth={key}
                position={{
                  xPos: movie.xPos,
                  yPos: movie.yPos
                }}
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
