from multiprocessing import Pool
import subprocess
import shlex
import os
import argparse


def f(seed):
    cmd = f"bin/prism {seed}"
    p = subprocess.run(cmd, shell=True, env=dict(os.environ, PRISM_MAINCLASS="prism.LearnVerify"))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("seeds", default=[1], type=int, nargs='+')
    parser.add_argument("--cpus", default=os.cpu_count(), type=int)
    args = parser.parse_args()
    with Pool(args.cpus) as p:
        p.map(f, args.seeds)
