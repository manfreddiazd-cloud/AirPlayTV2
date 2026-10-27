cmake_minimum_required(VERSION 3.22.1)
project(airplaytv LANGUAGES CXX)

add_library(airplaytv SHARED native-lib.cpp)

find_library(log-lib log)
target_link_libraries(airplaytv ${log-lib})
