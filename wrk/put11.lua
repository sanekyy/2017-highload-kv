counter = 0

request = function()
   wrk.method = "PUT"
   path = "/v0/entity?id=" .. counter .. "&replicas=2/3"
   wrk.body = counter
   counter = counter + 1
   return wrk.format(nil, path)
end
