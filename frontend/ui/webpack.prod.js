// @flow
const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = new Merge(CommonConfig, {
  output: {
    filename: '[name].[hash].js',
  },
  devtool: 'source-map',
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: '"production"'
      }
    }),
    new HtmlWebpackPlugin({
      template: './src/main/assets/index.html'
    }),
    new webpack.LoaderOptionsPlugin({
      minimize: true,
      debug: false
    }),
    new webpack.optimize.UglifyJsPlugin({
      beautify: false,
      compress: {
        screw_ie8: true
      },
      comments: false,
      sourceMap: false,
    }),
  ]
});