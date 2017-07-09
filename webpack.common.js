const webpack = require('webpack');
const path = require('path');
const source = path.resolve(__dirname, 'src/assets/js/pages');

module.exports = {
  entry: {
    discover: source + '/discover.js',
    history: source + '/history.js',
    home: source + '/home.js',
    login: source + '/login.js',
    movies: source + '/movies.js',
    series: source + '/series.js',
    stats: source + '/stats.js'
  },
  output: {
    filename: '[name].js',
    path: path.resolve(__dirname, 'src/main/dist/static')
  },
  resolve: {
    modules: [
      path.resolve(__dirname, 'build/dependencies'),
      path.resolve(__dirname, 'database/src/main/gen/database-js'),
      path.resolve(__dirname, 'mail/src/main/gen/mail-js'),
      path.resolve(__dirname, 'omdb/src/main/gen/omdb-js'),
      path.resolve(__dirname, 'tmdb/src/main/gen/tmdb-js'),
      'node_modules'
    ],
    alias: {
      database$: path.resolve(__dirname, 'database/src/main/gen/database-js/database_service.js'),
      mail$: path.resolve(__dirname, 'mail/src/main/gen/mail-js/mail_service.js'),
      omdb$: path.resolve(__dirname, 'omdb/src/main/gen/omdb-js/omdb_service.js'),
      tmdb$: path.resolve(__dirname, 'tmdb/src/main/gen/tmdb-js/tmdb_service.js'),
    }
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: 'babel-loader?cacheDirectory=true'
      },
      /*      {
       test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
       loader: 'url-loader?limit=10000&mimetype=application/font-woff'
       },
       {
       test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
       loader: 'file-loader'
       }*/
    ]
  },
  plugins: [
    /*new webpack.optimize.CommonsChunkPlugin({ //todo separate dependencies properly
     name: 'vendor',
     filename: 'vendor.min.js',
     minChunks: Infinity
     })*/
    new webpack.ProvidePlugin({
      $: 'jquery',
      jQuery: 'jquery',
      'window.jQuery': 'jquery'
    })
  ]
};