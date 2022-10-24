# LUIaard

Implementation of 'Linearly Updating Intervals' (LUI) for Robust Anytime Learning of Markov Decision Processes (NeurIPS 2022).

---

## Developers

This implementation was build in Java on top of PRISM (probabilistic model checker), see  http://www.prismmodelchecker.org for more.

Main developers:
* Marnix Suilen
* Thiago D. Simao

Contributers:
* David Parker
* Nils Jansen

---


## Usage

To run the experiments from the paper:

 * enter the PRISM directory and type `cd prism` then `make`
 * run `PRISM_MAINCLASS=prism.LearnVerify bin/prism` from the same directory
 * `python run.py $(seq -s \  0 99) --cpus 2`

This will run all experiments from the paper with a fixed seed.run.
The output is saved on the folder `prism/results`.

## Modifying experiments

To modify the experiments open prism/src/prism/LearnVerify.java

 * to run with random seed: set `FIXED_SEED` to `false`

to modify the experiments, go to the `main` method

 * for a basic comparison between algorithm use the method `run_basic_algorithms`
 * to analyze the effects of the prior strength use the method `strength_evaluation;`

All experiments reset the seed if the seed is fixed, i.e., multiple experiments can safely be defined and enabled right after each other.

Note that whenever the source code changes, you have to run `make` again.

## Datasets (models)

The models can be found in the `prism/models` directory.


***
