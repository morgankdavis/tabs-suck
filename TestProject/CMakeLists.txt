cmake_minimum_required(VERSION 3.17)
project(TestProject)

set(CMAKE_CXX_STANDARD 14)

add_executable(TestProject main.cpp a.cxx a.h b.cpp b.hh c.cc c.hpp d.c d.h e.cxx e.hxx)