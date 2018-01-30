import React from 'react';
import '../static/css/index.css';
import FontAwesome from 'react-fontawesome';

export default class ListPoster extends React.Component {

  constructor(props) {
    super(props);
  }
/*
<span className={'hoverOpen'}>Open</span>*/
  render() {
    return (
        <div className={'boxContainer'}>
          <div className="listBox">
            <div className={'hoverBackground'}></div>
            <div className={'hoverRating'}>{this.props.movieRating}</div>
            <span className={'hoverTitle'}>{this.props.movieTitle}</span>
            <div className={'hoverBorder'}></div>
            <FontAwesome
                className={'posterEye'}
                name={'times'}
                size={'lg'}
            />
            <img src={this.props.moviePosterPath} className={'listPosterImg'} alt=""/>
          </div>
        </div>
    );
  }
}
