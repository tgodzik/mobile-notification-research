from pysimplesoap.client import SoapClient, SoapFault

# create a simple consumer
client = SoapClient(
    location="http://localhost:8008/",
    action='http://localhost:8008/',
    namespace="http://example.com/sample.wsdl",
    soap_ns='soap',
    ns=False)


# call the remote method
response = client.Time(a=0)
# extract and convert the returned value
result = response.TimeResult

print int(result)