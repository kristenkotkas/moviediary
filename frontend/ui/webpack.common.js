// @flow
const webpack = require('webpack');
const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const PATHS = {
  root: path.resolve(__dirname),
  nodeModules: path.resolve(__dirname, './node_modules'),
  src: path.resolve(__dirname, './src/main/assets'),
  dist: path.resolve(__dirname, './src/main/dist/static'),
  styles: path.resolve(__dirname, 'src/main/resources/css'),
  images: path.resolve(__dirname, 'src/main/resources/img'),
  fonts: path.resolve(__dirname, 'src/main/resources/fonts')
};

module.exports = {
  cache: true,
  context: PATHS.root,
  entry: PATHS.src + '/client.js',
  output: {
    publicPath: '/',
    path: PATHS.dist
  },
  resolve: {
    alias: {
      css: PATHS.styles,
      img: PATHS.images
    },
    extensions: ['.js', '.jsx', '.json'],
    modules: ['src', 'node_modules']
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: (module) => module.context && module.context.indexOf('node_modules') !== -1
    }),
    new webpack.optimize.CommonsChunkPlugin({
      name: 'manifest'
    }),
    new HtmlWebpackPlugin({
      template: PATHS.src + '/index.html'
    }),
    new ExtractTextPlugin('[name]/styles.[contenthash].css')
  ],
  module: {
    rules: [
      // es6 + flow
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        loader: 'babel-loader',
        query: {
          presets: ['react', 'es2015', 'stage-0', 'flow'],
          plugins: ['react-html-attrs', 'transform-class-properties', 'transform-decorators-legacy']
        }
      },
      // json
      {
        test: /\.json$/,
        include: [PATHS.src],
        use: {loader: 'json-loader'}
      },
      // css
      {
        test: /\.css$/,
        include: [PATHS.styles],
        loader: ExtractTextPlugin.extract([
          'css-loader?{modules: false}',
          'postcss-loader'
        ])
      },
      // less
      {
        test: /\.less$/,
        include: [PATHS.styles],
        loader: ExtractTextPlugin.extract([
          'css-loader?{modules: false}',
          'less-loader'
        ])
      },
      // images
      {
        test: /\.(jpg|jpeg|png|gif|svg)$/,
        include: [PATHS.images],
        use: {
          loader: 'url-loader',
          options: {
            name: 'images/[hash].[ext]',
            limit: 1000 // inline file data until size
          }
        }
      },
      // fonts
      {
        test: /\.(woff|woff2|ttf|eot)$/,
        include: [
          PATHS.fonts
        ],
        use: {
          loader: 'file-loader',
          options: {
            name: 'fonts/[name].[hash].[ext]'
          }
        }
      }
    ]
  }
};