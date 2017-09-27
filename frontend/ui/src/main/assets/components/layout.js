// @flow
import React from 'react';
import {connect} from 'react-redux';

import {fetchItem} from '../actions/action';

type Props = {
  dispatch: any,
  items: any,
  myString: String
}

@connect((store) => {
  return {
    items: store.items,
    myString: store.inner.randomString
  };
})
export default class Layout extends React.Component<Props> {
  fetchNextItem() {
    this.props.dispatch(fetchItem());
  }

  render() {
    const items = this.props.items;
    const mappedItems = items.map(item => <li key={item.id}>{item.name + this.props.myString}</li>);

    return <div>
      <h1>Hello!</h1>
      <button onClick={this.fetchNextItem.bind(this)}>Click me</button>
      <ul>{mappedItems}</ul>
    </div>;
  }
}