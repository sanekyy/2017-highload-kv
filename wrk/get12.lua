counter = 0

request = function()
   wrk.method = "GET"
   path = "/v0/entity?id=" .. counter .. "&replicas=3/3"
   counter = counter + 1
   return wrk.format(nil, path)
end

