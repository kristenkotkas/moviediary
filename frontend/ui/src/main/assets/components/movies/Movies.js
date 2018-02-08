// @flow
import React from 'react';
import {connect} from 'react-redux';
import '../../../resources/css/Movies.css'
import {Grid} from "semantic-ui-react";
import GenreLabel from "./GenreLabel";

type Props = {}

const genres = ['Mystery', 'Science Fiction', 'Thriller'];

@connect((store) => {
  return {};
})
export default class Movies extends React.Component<Props> {

  render() {
    return (
        <Grid centered columns={2}>
          <Grid.Row columns={2}>
            <Grid.Column>
              <div className={'moviesProfileCard'}>
                <img className={'moviesPoster'}
                     src="https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg" alt="poster"/>
                <span className={'moviesYear'}>2017</span>
                <span className={'movieTitle'}>Blade Runner: 2049</span>
                <span className={'moviesMetaContainer'}>
                <span className={'movieRuntime'}>163 min</span>
                <span className={'movieRelease'}>4 October 2017</span>
                </span>
                <span className={'genreContainer'}>
                {genres.map((genre, key) => {
                  return <GenreLabel genre={genre} key={key}/>
                })}
                </span>
              </div>
            </Grid.Column>
          </Grid.Row>
        </Grid>
    );
  }
}