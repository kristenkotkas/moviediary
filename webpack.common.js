const webpack = require('webpack');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const resources = path.resolve(__dirname, 'src/main/resources');

module.exports = {
  entry: path.resolve(__dirname, 'src/main/assets/main.js'),
  output: {
    filename: 'bundle.js',
    publicPath: '/static/',
    path: path.resolve(__dirname, 'src/main/dist/static')
  },
  resolve: {
    alias: {
      css: resources + '/css',
      img: resources + '/img',
      vue$: 'vue/dist/vue.esm.js'
    }
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: 'src/main/assets/index.hbs'
    })
  ],
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        options: {
          loaders: {
            'scss': 'vue-style-loader!css-loader!sass-loader',
            'sass': 'vue-style-loader!css-loader!sass-loader?indentedSyntax'
          }
        }
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        exclude: /node_modules/
      },
      {
        test: /\.(png|jpg|gif|svg)$/,
        loader: 'file-loader',
        options: {
          objectAssign: 'Object.assign'
        }
      },
      {
        test: /\.styl$/,
        loader: ['style-loader', 'css-loader', 'stylus-loader']
      },
      {
        test: /\.hbs$/,
        loader: 'handlebars-loader'
      },
    ]
  }
};