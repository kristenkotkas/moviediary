// @flow
export default function reducer(state = {
  items: [],
  inner: {
    randomString: 'abc'
  }
}, action) {

  switch (action.type) {
    case 'FETCH_ITEM': {
      const newItems = [...state.items];
      newItems.push({
        id: Date.now(),
        name: Date.now()
      });
      return {...state, items: newItems};
    }
  }

  return state;
}