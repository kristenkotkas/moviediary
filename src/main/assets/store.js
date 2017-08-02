import {applyMiddleware, createStore} from "redux"
import {composeWithDevTools} from 'redux-devtools-extension';

import {createLogger} from "redux-logger"
import thunk from "redux-thunk"
import promise from "redux-promise-middleware"

import reducer from "./reducers/reducer"

const middleware = applyMiddleware(promise(), thunk, createLogger());

const composeEnhancers = composeWithDevTools({
  //redux devtools options, https://github.com/zalmoxisus/redux-devtools-extension
});

export default createStore(reducer, composeEnhancers(middleware));