const webpack = require('webpack');
const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

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
  entry: {
    app: [
      'react-hot-loader/patch',
      './src/main/assets/client.js'
    ]
  },
  output: {
    publicPath: '/',
    path: PATHS.dist
  },
  resolve: {
    alias: {
      css: PATHS.styles,
      img: PATHS.images
    },
    extensions: ['.ts', '.tsx', '.js', '.jsx', '.json'],
    modules: ['src', 'node_modules']
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: (module) => module.context && module.context.indexOf('node_modules') !== -1,
    }),
    new webpack.optimize.CommonsChunkPlugin({
      name: 'manifest',
    }),
  ],
  module: {
    rules: [
      // typescript
      {
        test: /\.tsx?$/,
        include: PATHS.src,
        exclude: /node_modules/,
        use: [
          {loader: 'react-hot-loader/webpack'},
          {
            loader: 'awesome-typescript-loader',
            options: {
              transpileOnly: true,
              useTranspileModule: false,
              sourceMap: true, //todo only on dev
            },
          }
        ]
      },
      // json
      {
        test: /\.json$/,
        include: [PATHS.src],
        use: {loader: 'json-loader'},
      },
      // css
      {
        test: /\.css$/,
        include: [PATHS.styles],
        loader: ExtractTextPlugin.extract([
          'css-loader?{modules: false}',
          'postcss-loader',
        ]),
      },
      // less
      {
        test: /\.less$/,
        include: [PATHS.styles],
        loader: ExtractTextPlugin.extract([
          'css-loader?{modules: false}',
          'less-loader',
        ]),
      },
      // images
      {
        test: /\.(jpg|jpeg|png|gif|svg)$/,
        include: [PATHS.images],
        use: {
          loader: 'url-loader',
          options: {
            name: 'images/[hash].[ext]',
            limit: 1000, // inline file data until size
          },
        },
      },
      // fonts
      {
        test: /\.(woff|woff2|ttf|eot)$/,
        include: [
          PATHS.fonts,
        ],
        use: {
          loader: 'file-loader',
          options: {
            name: 'fonts/[name].[hash].[ext]',
          },
        },
      },
    ]
  }
};