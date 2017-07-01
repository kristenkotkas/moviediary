'use strict';

var gulp = require('gulp');
var browserify = require('browserify');
var babelify = require('babelify');
var source = require('vinyl-source-stream');
var buffer = require('vinyl-buffer');
var uglify = require('gulp-uglify');
var sourcemaps = require('gulp-sourcemaps');
var livereload = require('gulp-livereload');

gulp.task('build', function () {
    return browserify({entries: 'src/assets/javascripts/asset-test.js', debug: true})
        .transform('babelify', {presets: ['es2015']})
        .bundle()
        .pipe(source('app.js'))
        .pipe(buffer())
        .pipe(sourcemaps.init())
        .pipe(uglify())
        .pipe(sourcemaps.write('build/assets/maps'))
        .pipe(gulp.dest('build/assets/js'))
        .pipe(livereload());
});

gulp.task('watch', ['build'], function () {
    livereload.listen();
    gulp.watch('src/assets/javascripts/*.js', ['build']);
});

gulp.task('default', ['watch']);