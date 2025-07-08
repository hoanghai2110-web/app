cmake_minimum_required(VERSION 3.22.1)

project("llama_wrapper")

# Simple wrapper without heavy llama.cpp
add_library(
    llama_wrapper
    SHARED
    llama_wrapper.cpp
)

# Basic libraries
find_library(log-lib log)

target_link_libraries(
    llama_wrapper
    ${log-lib}
)