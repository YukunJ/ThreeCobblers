#!/usr/bin/env python3

# make sure you run 'pip3 install -r requirements.txt' first
"""
This is the test driver for microservice-1 QR Code functionality
You need to have your web service actively running already
It sends http request with the same data payload
    to both reference website and our web service
And compare if the responses match with each other
    and calculate statistics accordingly
"""
import requests
import string

"""
@brief: not a complete list of escape processing for a query payload
@return: partially escaped data for url sending
"""
def escape(data):
    for char in string.punctuation:
        data = data.replace(char, '')
    return data.replace(' ', '%20').replace("""’""", "%27")

"""
@brief : send http request to get an response
         might fail if the url is not well formed
@param : request_url fully filled request url
@return : the response if any (might be empty if fails)
"""
def http_request(request_url):
    response = requests.request("get", request_url)
    return response.text

"""
@brief : encode functionality driver
@param : test_suite list of random sentences to be encoded
@param : reference_website the official 'correct' implementation refernce
@param : my_website our microservice web service IP address
@param : encode_prefix the prefix in http request to indicate encode
"""
def test_decode(test_suite, reference_website, my_website, encode_prefix):
    log = ""
    count, correct, error, valid_length, invalid_length = 0, 0, 0, 0, 0
    for i in range(len(test_suite)):
        request_url_ref = reference_website + encode_prefix + escape(test_suite[i])
        response_ref = http_request(request_url_ref)
        if response_ref != "":
            valid_length += 1
        else:
            invalid_length += 1
        print("test " + str(i+1) + "/" + str(len(test_suite)), end = ' ')
        request_url_my = my_website + encode_prefix + escape(test_suite[i])
        response_my = http_request(request_url_my)
        if response_ref == response_my:
            correct += 1
            print("pass")
        else:
            print("fail")
            error += 1
            log += "==============================================\n"
            log += "Reference Request url: |" + request_url_ref + "|\n"
            log += "My Request url: |" + request_url_my + "|\n"
            log += "reference answer: |" + response_ref + "|\n"
            log += "my answer: |" + response_my + "|\n\n"
    print("Statistics: total {}, correct {}, error {}, valid_lenth {}, invalid_length {}".format(count, correct, error, valid_length, invalid_length))
    if error > 0:
        print("Here is the error Log:")
        print("Be careful about the how special character is escaped\n It might not be your fault!")
        print(log)

if __name__ == "__main__":

    # TODO: update your own parameters here
    reference_website = "http://reference.sailplatform.org"
    port = "8080" # the port web application is listening on
    my_website = "http://localhost" + ":" + port # localhost or external IP
    test_file = "./test_suite.txt"
    encode_prefix = "/qrcode?type=encode&data="
    decode_prefix = "/qrcode?type=decode&data="
    
    # display parameters
    print("reference website: " + reference_website)
    print("application port: " + port)
    print("web service website:" + my_website)
    print("test suite is located at: " + test_file)
    
    # load in the test suite from local file
    with open(test_file, 'r', encoding='utf-8') as f:
        test_suite = [line.strip('\n') for line in f.readlines()]
        
    print("==========Begin Encode functionality Test===================")
    test_decode(test_suite, reference_website, my_website, encode_prefix)
    print("===========End Encode functionality Test===================")
