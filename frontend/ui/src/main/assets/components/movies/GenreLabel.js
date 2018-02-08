// @flow
import React from 'react';
import {connect} from 'react-redux';
import '../../../resources/css/Movies.css'

type Props = {
  genre: string
}

const genres = ['Mystery', 'Science Fiction', 'Thriller'];

@connect((store) => {
  return {};
})
export default class GenreLabel extends React.Component<Props> {

  render() {
    return (
        <span className={'moviesGenreName'}>{this.props.genre}</span>
    );
  }
}