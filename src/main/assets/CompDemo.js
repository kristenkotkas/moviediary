import React from 'react';
import ListPoster from "./components/ListPoster";

export default class CompDemo extends React.Component {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div>
        <h1>Comp demo</h1>
        <ListPoster
          movieTitle={"Blade Runner 2049"}
          moviePosterPath={"https://image.tmdb.org/t/p/w640/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg"}
          movieRating={'5.2'}
        />
      </div>
    );
  }
}
