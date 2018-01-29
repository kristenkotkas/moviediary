import React from 'react';
import '../static/css/index.css';
import FontAwesome from 'react-fontawesome';

export default class ListPoster extends React.Component {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div className="listBox" style={{
        position: 'absolute',
      }}>
        <div className={'hoverBackground'}></div>
        <div className={'hoverRating'}>{this.props.movieRating}</div>
        <div className={'hoverTitle'}>{this.props.movieTitle}</div>
        <span className={'hoverOpen'}>Open</span>
        <FontAwesome
          className={'posterEye'}
          name={'eye'}
          size={'2x'}
        />
        <div className={'hoverBorder'}></div>
        <img src={this.props.moviePosterPath} className={'listPosterImg'} alt=""/>
      </div>
    );
  }
}
