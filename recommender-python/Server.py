#!/usr/bin/env python

import urllib
from http.server import BaseHTTPRequestHandler, HTTPServer


class HttpServer(BaseHTTPRequestHandler):

    def do_GET(self):
        path = self.path
        params = urllib.parse.parse_qs(path[2:])
        if not params:
            self.send_response(404)
            return
        movie_id = params['id'][0]

        response = "{\"hello\": \"world\", \"movie_id: \"" + movie_id + "\"}"

        # todo return response json

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(bytes(response, "utf8"))
        return


def run():
    print('starting server...')
    server_address = ('127.0.0.1', 9998)
    httpd = HTTPServer(server_address, HttpServer)
    print('running server...')
    httpd.serve_forever()


run()
# example GET http://localhost:9998/?id=1
