import React from 'react';
import '../static/css/index.css';
import FontAwesome from 'react-fontawesome';

export default class ListPoster extends React.Component {

  constructor(props) {
    super(props);
  }
/*
<span className={'hoverOpen'}>Open</span>*/

  removeMovie() {
    this.props.removeCallback(this.props.movieId);
  }

  openMovie() {
    this.props.openCallback(this.props.movieId);
  }

  render() {
    return (
        <div className={'boxContainer'}>
          <div className={'listBox'}>
            <div className={'hoverBackground'}></div>
            <span className={'hoverYear'}>{this.props.movieYear}</span>
            <span className={'hoverTitle'}>{this.props.movieTitle}</span>
            <span className={'hoverRating'}>{this.props.movieRating}</span>
            {this.props.movieSeen ? <span className={'hoverSeen'}></span> : null}
            <div className={'hoverBorder'}></div>
            <span className={'hoverOpen'} onClick={this.openMovie.bind(this)}>Open</span>
            <FontAwesome
                className={'posterClose'}
                name={'times'}
                size={'lg'}
                onClick={this.removeMovie.bind(this)}
            />
            <img src={this.props.moviePosterPath} className={'listPosterImg'} alt=""/>
          </div>
        </div>
    );
  }
}
