# LUIaard

Implementation of 'Linearly Updating Intervals' (LUI) for [Robust Anytime Learning of Markov Decision Processes](https://arxiv.org/abs/2205.15827) (NeurIPS 2022).

![lui](https://github.com/lava-lab/luiaard/blob/master/assets/lui.gif?raw=true)

---

## Developers

This implementation was build in Java on top of PRISM (probabilistic model checker), see  http://www.prismmodelchecker.org for more.

Main developers:
* Marnix Suilen
* Thiago D. Simão

Contributers:
* David Parker
* Nils Jansen

---


## Usage

To run the experiments from the paper:

 * enter the PRISM directory and type `cd prism` then `make`.
 * run `python run.py $(seq -s \  0 99) --cpus 2` to run all experiments presented in the paper for seeds 0..99 in parallel on 2 CPUs.

The output is saved in the folder `prism/results` in seperate files/folders for each experiments and seed.

The `prism/notebooks` directory contains python scripts to plot the results.

## Datasets (models)

The models can be found in the `prism/models` directory.


***
