import React from 'react';
import '../static/css/App.css';
import Draggable from 'react-draggable';

export default class DraggablePoster extends React.Component {

  constructor(props) {
    super(props);
    const randomPos = this.getRandomPosition(0.15, 0.85);
    this.state = {
      imageXPosition: randomPos.xPos,
      imageYPosition: randomPos.yPos,
      movieTitle: props.movieTitle,
      movieId: props.movieId,
      moviePosterPath: props.moviePosterPath,
      posterStyle: {
        position: 'absolute',
        zIndex: this.props.depth
      }
    };
  }

  getRandomPosition(min, max) {
    let xPos = Math.random() * (max - min) + min;
    let yPos = Math.random() * max;
    return {
      xPos: this.props.screenSize.screenWidth * xPos,
      yPos: this.props.screenSize.screenHeight * yPos
    };
  }

  getPosition(xPos, yPos) {
    return {
      xPos: this.props.screenSize.screenWidth * xPos,
      yPos: this.props.screenSize.screenHeight * yPos
    };
  }

  handleStop(draggableEventHandler) {
    /*console.log(
        draggableEventHandler.clientX,
        draggableEventHandler.clientY
    );*/
    this.setState({
      imageXPosition: draggableEventHandler.clientX,
      posterStyle: {...this.state.posterStyle, zIndex: this.props.depth}
    }, this.handleMovieLike.bind(this));
  }

  handleStart(draggableEventHandler) {
    this.setState({
      posterStyle: {...this.state.posterStyle, zIndex: 999}
    });
  }

  handleMovieLike() {
    this.props.stopHandler({
      state: this.state,
      data: this.props.data
    });
  }

  render() {
    const randomPos = this.getPosition(this.props.position.xPos, this.props.position.yPos);
    let position = this.props.position.xPos === 0 ?
        {x: this.state.imageXPosition, y: this.state.imageYPosition} :
        {x: randomPos.xPos, y: randomPos.yPos};
    return (
        <Draggable
            defaultPosition={{x: this.state.imageXPosition, y: this.state.imageYPosition}}
            position={position}
            bounds="body"
            onStop={this.handleStop.bind(this)}
            onStart={this.handleStart.bind(this)}
        >
          <div
              style={this.state.posterStyle} className="box">
            <img src={this.state.moviePosterPath} alt=""/>
          </div>
        </Draggable>
    );
  }
}
