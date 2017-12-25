const webpack = require('webpack');
const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const paths = {
  nodeModules: path.resolve(__dirname, './node_modules'),
  src: path.resolve(__dirname, './src/main/assets'),
  dist: path.resolve(__dirname, './src/main/resources/static/dist'),
  css: path.resolve(__dirname, './src/main/assets/static/css'),
  images: path.resolve(__dirname, './src/main/assets/static/img'),
  fonts: path.resolve(__dirname, './src/main/assets/static/fonts')
};

module.exports = {
  cache: true,
  context: paths.src,
  entry: paths.src + '/index.js',
  output: {
    publicPath: '/',
    path: paths.dist
  },
  resolve: {
    alias: {
      css: paths.css,
      img: paths.images
    },
    extensions: ['.js', '.jsx', '.json']
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
      template: paths.src + '/index.html'
    }),
    new ExtractTextPlugin('[name]/styles.[contenthash].css')
  ],
  module: {
    rules: [
      {
        test: /\.js?$/,
        exclude: /node_modules/,
        loader: 'babel-loader',
        query: {
          presets: ['react', 'es2015', 'stage-0'],
          plugins: ['react-html-attrs']
        }
      },
      {
        test: /\.css$/,
        include: [paths.css, /node_modules/],
        use: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: [
            {
              loader: 'css-loader'
            }
          ]
        })
      },
      {
        test: /\.(jpg|jpeg|png|gif|svg)$/,
        include: [paths.images],
        use: {
          loader: 'url-loader',
          options: {
            name: 'images/[hash].[ext]',
            limit: 1000
          }
        }
      }
    ]
  }
};
