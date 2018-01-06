import React from 'react';
import '../static/css/App.css';
import FontAwesome from 'react-fontawesome';

export default class SidePoster extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      movieTitle: props.movieTitle,
      movieId: props.movieId,
      moviePosterPath: props.moviePosterPath
    };
  }

  removeMovie() {
    this.props.removeMovie(this.state);
  }

  /*

          <FontAwesome
              className={'removeMovie'}
              name={'times'}
              onClick={this.removeMovie.bind(this)}
          />
   */

  render() {
    return (
        <div className="box" style={{
          position: 'absolute',
          transform: 'translate(' + this.props.xPos + 'px, ' + this.props.yPos + 'px)'
        }}>
          <img src={this.state.moviePosterPath} className={'sidePosterImg'} alt=""/>
        </div>
    );
  }
}
