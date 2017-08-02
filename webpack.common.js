const webpack = require('webpack');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const resources = path.resolve(__dirname, 'src/main/resources');

module.exports = {
  entry: './src/main/assets/client.js',
  output: {
    filename: 'bundle.js',
    publicPath: '/',
    path: path.resolve(__dirname, 'src/main/dist/static')
  },
  resolve: {
    alias: {
      css: resources + '/css',
      img: resources + '/img'
    }
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './src/main/assets/index.hbs'
    })
  ],
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        loader: 'babel-loader',
        query: {
          presets: ['react', 'es2015', 'stage-0'],
          plugins: ['react-html-attrs', 'transform-class-properties', 'transform-decorators-legacy'],
        }
      },
      {
        test: /\.hbs$/,
        loader: 'handlebars-loader'
      },
    ]
  }
};