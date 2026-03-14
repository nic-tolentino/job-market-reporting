const http = require('http');

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html' });
  
  if (req.url === '/jobs') {
    res.end('<html><body><a href="/jobs?page=2" rel="next">Next</a></body></html>');
  } else if (req.url.startsWith('/jobs?page=')) {
    const page = parseInt(req.url.split('=')[1]);
    if (page < 60) {
      res.end(`<html><body><a href="/jobs?page=${page + 1}" rel="next">Next</a></body></html>`);
    } else {
      res.end('<html><body>End of pagination</body></html>');
    }
  } else {
    res.end('<html><body>Test Server</body></html>');
  }
});

server.listen(8082, () => console.log('Mock target server running on 8082'));
